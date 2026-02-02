package com.fedd.salesforce.tests;

import org.junit.jupiter.api.Test;

/**
 * JUnit test runner for Stress Test Plan
 * 
 * Executes breaking point analysis to find Salesforce Developer Edition concurrent request limit.
 * 
 * To run:
 * mvn test -Dtest=StressTest
 * 
 * Expected duration: ~15 minutes
 * Expected API calls: ~300-450
 */
public class StressTest {
    
    @Test
    public void testStressPlan() throws Exception {
        new StressTestPlan().getTestPlan().run();
    }
}
