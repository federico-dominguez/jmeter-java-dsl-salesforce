package com.fedd.salesforce.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static us.abstracta.jmeter.javadsl.JmeterDsl.httpCache;
import static us.abstracta.jmeter.javadsl.JmeterDsl.httpCookies;
import static us.abstracta.jmeter.javadsl.JmeterDsl.testPlan;
import static us.abstracta.jmeter.javadsl.JmeterDsl.vars;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fedd.salesforce.scenarios.AuthenticationSetupThreadGroup;
import com.fedd.salesforce.scenarios.CleanUpTeardownThreadGroup;

import us.abstracta.jmeter.javadsl.core.TestPlanStats;

/**
 * Manual Cleanup Test
 * 
 * Runs only the cleanup thread group to delete all test records from Salesforce.
 * Use this when:
 * - Storage limit exceeded during spike/stress tests
 * - Need to clean up orphaned test data
 * - Preparing for a fresh test run
 * 
 * Execution:
 * mvn test -Dtest=CleanupTest
 * 
 * Warning: This will delete ALL records created by the test framework
 * (Leads, Accounts, Opportunities, Tasks, Cases, Events, Notes with specific naming patterns)
 */
public class CleanupTest {
    
    private final AuthenticationSetupThreadGroup authGroup = new AuthenticationSetupThreadGroup();
    private final CleanUpTeardownThreadGroup cleanUpGroup = new CleanUpTeardownThreadGroup();
    
    @Test
    public void runCleanup() throws IOException {
        System.out.println("Starting cleanup of all test records...");
        
        TestPlanStats stats = testPlan(
            vars().set("BASE_URL", "orgfarm-b8d4a27e18-dev-ed.develop.my.salesforce.com")
                  .set("ownerId", "005gK00000AQ0ppQAD"),
            httpCache().disable(),
            authGroup.getSetupThreadGroup(),  // Authenticate first
            httpCookies().disable(),
            cleanUpGroup.getTeardownThreadGroup()
        ).run();
        
        System.out.println("Cleanup completed!");
        System.out.println("Total requests: " + stats.overall().samplesCount());
        System.out.println("Errors: " + stats.overall().errorsCount());
        
        // Cleanup should complete even if some records don't exist
        assertThat(stats.overall().samplesCount()).isGreaterThan(0);
    }
}
