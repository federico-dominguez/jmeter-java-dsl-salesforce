package com.fedd.salesforce.tests;

import com.fedd.salesforce.plans.SpikeTestPlan;

import org.junit.jupiter.api.Test;

/**
 * JUnit test runner for Spike Test Plan
 * 
 * Executes traffic surge validation with baseline → spike → recovery pattern.
 * 
 * To run:
 * mvn test -Dtest=SpikeTest
 * 
 * Expected duration: ~5-7 minutes
 * Expected API calls: ~150-200
 */
public class SpikeTest {
    
    @Test
    public void testSpikePlan() throws Exception {
        new SpikeTestPlan().getTestPlan().run();
    }
}
