package com.fedd.salesforce.tests;

import org.junit.jupiter.api.Test;

/**
 * JUnit test runner for Volume Test Plan
 * 
 * Executes large dataset performance test with 100 unique lead records.
 * 
 * To run:
 * mvn test -Dtest=VolumeTest
 * 
 * Expected duration: ~7-10 minutes
 * Expected API calls: ~500
 * 
 * NOTE: Uses volume_leads_data.csv with 100 unique records.
 * Compare results against baseline test to measure scaling impact.
 */
public class VolumeTest {
    
    @Test
    public void testVolumePlan() throws Exception {
        new VolumeTestPlan().getTestPlan().run();
    }
}
