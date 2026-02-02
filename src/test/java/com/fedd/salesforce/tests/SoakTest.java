package com.fedd.salesforce.tests;

import org.junit.jupiter.api.Test;

/**
 * JUnit test runner for Soak Test Plan
 * 
 * Executes long-duration stability test to detect memory leaks and performance degradation.
 * 
 * To run:
 * mvn test -Dtest=SoakTest
 * 
 * Expected duration: ~1 hour 5 minutes
 * Expected API calls: ~1,500
 * 
 * WARNING: This test runs for 1 hour. Ensure you have time before starting.
 * Monitor Grafana dashboard during execution to watch for performance trends.
 */
public class SoakTest {
    
    @Test
    public void testSoakPlan() throws Exception {
        new SoakTestPlan().getTestPlan().run();
    }
}
