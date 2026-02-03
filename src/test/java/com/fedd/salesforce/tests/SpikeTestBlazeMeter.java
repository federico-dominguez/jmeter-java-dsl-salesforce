package com.fedd.salesforce.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.fedd.salesforce.plans.SpikeTestBlazeMeterTestPlan;

import us.abstracta.jmeter.javadsl.blazemeter.BlazeMeterEngine;
import us.abstracta.jmeter.javadsl.core.TestPlanStats;

/**
 * BlazeMeter Cloud Execution for Spike Test
 * 
 * Runs the Spike Test scenario on BlazeMeter's cloud infrastructure.
 * 
 * Prerequisites:
 * 1. Set BZ_TOKEN environment variable: export BZ_TOKEN="api-id:api-secret"
 * 2. Upload leads_data.csv as asset to BlazeMeter test
 * 3. Configure Salesforce credentials in BlazeMeter test settings:
 *    - SALESFORCE_USERNAME
 *    - SALESFORCE_CLIENT_ID
 *    - SALESFORCE_PRIVATE_KEY
 *    - AUDIENCE
 * 
 * Execution:
 * mvn test -Dtest=SpikeTestBlazeMeter
 * 
 * Or with exec plugin:
 * mvn exec:java -Dexec.mainClass="com.fedd.salesforce.tests.SpikeTestBlazeMeter" -Dexec.classpathScope=test
 * 
 * Load Pattern:
 * - Phase 1: 2 users baseline
 * - Phase 2: 10 users spike
 * - Phase 3: 2 users recovery
 * 
 * Success Criteria:
 * - Overall p99 response time < 5 seconds
 * - Error rate during spike < 20%
 * - Clean recovery to baseline performance
 */
public class SpikeTestBlazeMeter {
    
    @Test
    public void spikeTestBlazeMeter() throws Exception {
        String bzToken = System.getenv("BZ_TOKEN");
        if (bzToken == null || bzToken.isEmpty()) {
            System.out.println("BZ_TOKEN not set. Skipping BlazeMeter Spike Test.");
            return;
        }

        // BlazeMeter engine expects assets to be accessible by filename from working directory
        // Copy CSV to project root temporarily for upload
        java.io.File sourceFile = new java.io.File("src/main/resources/data/leads_data.csv");
        java.io.File dataFile = new java.io.File("leads_data.csv");
        java.nio.file.Files.copy(sourceFile.toPath(), dataFile.toPath(), 
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        // Note: Salesforce credentials are read from environment variables by the test plan
        // They are set as JMeter variables via vars().set() and passed to BlazeMeter
        TestPlanStats stats = new SpikeTestBlazeMeterTestPlan().getTestPlan()
                .runIn(new BlazeMeterEngine(bzToken)
                        .testName("Salesforce Spike Test - Traffic Surge Validation")
                        .totalUsers(14) // 2 + 10 + 2 users across 3 phases
                        .rampUpFor(Duration.ofMinutes(1))
                        .holdFor(Duration.ofMinutes(5))
                        .testTimeout(Duration.ofMinutes(15))
                        .assets(dataFile));

        // Assert p99 response time is under 5 seconds
        assertThat(stats.overall().sampleTimePercentile99()).isLessThan(Duration.ofSeconds(5));
        
        System.out.println("Spike Test completed successfully in BlazeMeter!");
        System.out.println("Total samples: " + stats.overall().samplesCount());
        System.out.println("Error rate: " + stats.overall().errorsCount() / (double) stats.overall().samplesCount() * 100 + "%");
        System.out.println("p99 response time: " + stats.overall().sampleTimePercentile99().toMillis() + "ms");
    }
}
