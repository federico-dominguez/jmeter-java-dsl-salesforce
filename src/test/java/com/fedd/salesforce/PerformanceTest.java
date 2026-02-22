package com.fedd.salesforce;

import static org.assertj.core.api.Assertions.assertThat;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jtlWriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.fedd.salesforce.config.TestConfig;
import com.fedd.salesforce.plans.LeadToCashBlazeMeterTestPlan;
import com.fedd.salesforce.plans.LeadToCashTestPlan;

import us.abstracta.jmeter.javadsl.blazemeter.BlazeMeterEngine;
import us.abstracta.jmeter.javadsl.core.TestPlanStats;
import us.abstracta.jmeter.javadsl.core.listeners.DslViewResultsTree;

/**
 * Entry point for running Lead-to-Cash performance tests.
 * <p>
 * Use Maven profiles to select the execution target:
 * <ul>
 *   <li>{@code mvn test -Plocal}  — runs the local JMeter test with InfluxDB monitoring</li>
 *   <li>{@code mvn test -Pcloud}  — runs the BlazeMeter cloud test (requires BZ_TOKEN)</li>
 *   <li>{@code mvn test}          — default runs only local tests</li>
 * </ul>
 * </p>
 */
public class PerformanceTest {

    private static final LeadToCashTestPlan leadToCashTestPlan = new LeadToCashTestPlan();
    private static final LeadToCashBlazeMeterTestPlan leadToCashBlazePlan = new LeadToCashBlazeMeterTestPlan();

    /**
     * Runs the lead-to-cash test locally with the embedded JMeter engine.
     * <p>
     * Asserts zero errors and that the 99th percentile response time stays
     * below the configured SLA threshold.
     * </p>
     */
    @Test
    @Tag("local")
    public void test() throws IOException {
        TestPlanStats stats = leadToCashTestPlan.getTestPlan().children(
                jtlWriter("target/jtls"), new DslViewResultsTree()).run();

        // No errors allowed
        assertThat(stats.overall().errorsCount()).as("Total error count").isEqualTo(0L);

        // SLA: P99 response time must stay below 10 seconds
        assertThat(stats.overall().sampleTimePercentile99())
                .as("P99 response time")
                .isLessThan(Duration.ofSeconds(10));
    }

    /**
     * Runs the lead-to-cash test on BlazeMeter cloud infrastructure.
     * <p>
     * Requires the {@code BZ_TOKEN} environment variable to be set.
     * Use {@code mvn test -Pcloud} to run.
     * </p>
     */
    @Test
    @Tag("cloud")
    public void blazeMeterTest() throws IOException, InterruptedException, TimeoutException {
        String bzToken = TestConfig.BZ_TOKEN;
        if (bzToken == null || bzToken.isEmpty()) {
            throw new IllegalStateException(
                    "BZ_TOKEN environment variable is required for cloud tests. "
                    + "Set it before running with -Pcloud.");
        }

        // BlazeMeter engine expects assets to be accessible by filename from working directory
        File sourceFile = new File(TestConfig.CSV_PATH);
        File dataFile = new File(TestConfig.CSV_ASSET_FILENAME);
        Files.copy(sourceFile.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        TestPlanStats stats = leadToCashBlazePlan.getTestPlan()
                .runIn(new BlazeMeterEngine(bzToken)
                        .testName("Salesforce Lead to Cash Performance Test")
                        .totalUsers(20)
                        .rampUpFor(Duration.ofMinutes(5))
                        .holdFor(Duration.ofMinutes(5))
                        .assets(dataFile));

        assertThat(stats.overall().sampleTimePercentile99())
                .as("P99 response time")
                .isLessThan(Duration.ofSeconds(5));
    }
}