package com.fedd.salesforce.tests;

import static us.abstracta.jmeter.javadsl.JmeterDsl.csvDataSet;
import static us.abstracta.jmeter.javadsl.JmeterDsl.httpCache;
import static us.abstracta.jmeter.javadsl.JmeterDsl.httpCookies;
import static us.abstracta.jmeter.javadsl.JmeterDsl.influxDbListener;
import static us.abstracta.jmeter.javadsl.JmeterDsl.testPlan;
import static us.abstracta.jmeter.javadsl.JmeterDsl.vars;

import com.fedd.salesforce.scenarios.AuthenticationSetupThreadGroup;
import com.fedd.salesforce.scenarios.CleanUpTeardownThreadGroup;
import com.fedd.salesforce.scenarios.LeadToCashThreadGroup;

import java.io.IOException;
import java.time.Duration;

import us.abstracta.jmeter.javadsl.core.DslTestPlan;

/**
 * Spike Test Plan - Traffic Surge Validation
 * 
 * Purpose: Test system behavior during sudden traffic spikes and recovery
 * 
 * Load Pattern:
 * Phase 1 - Baseline: 2 users for 10 iterations (establish normal behavior)
 * Phase 2 - Spike: Ramp to 10 users in 10 seconds, hold for 5 iterations (sudden surge)
 * Phase 3 - Recovery: Drop to 2 users for 10 iterations (validate recovery)
 * 
 * Expected Behavior:
 * - Baseline phase should show normal response times
 * - Spike phase should trigger Salesforce rate limiting (503 errors or UNABLE_TO_LOCK_ROW)
 * - Recovery phase should return to normal performance within 1-2 minutes
 * 
 * Success Criteria:
 * - Measure error rate during spike (acceptable: < 20%)
 * - Validate recovery time (baseline response time restored within 2 minutes)
 * - Confirm p95 response time returns to pre-spike levels
 * - No errors during recovery phase
 * 
 * Real-World Scenario:
 * - Simulates sudden increase in sales team activity (e.g., end of quarter rush)
 * - Tests API rate limiting behavior and auto-scaling response
 * - Validates system doesn't crash under sudden load
 * 
 * Salesforce API Limits:
 * - Concurrent requests: Will likely hit the limit during spike
 * - Estimated total API calls: ~150-200 (well under daily limit)
 */
public class SpikeTestPlan {

    private final AuthenticationSetupThreadGroup authGroup = new AuthenticationSetupThreadGroup();
    private final LeadToCashThreadGroup leadToCashGroup = new LeadToCashThreadGroup();
    private final CleanUpTeardownThreadGroup cleanUpGroup = new CleanUpTeardownThreadGroup();

    public DslTestPlan getTestPlan() throws IOException {
        return testPlan()
                .children(
                        vars().set("BASE_URL",
                                "orgfarm-b8d4a27e18-dev-ed.develop.my.salesforce.com")
                                .set("ownerId", "005gK00000AQ0ppQAD"),
                        csvDataSet(
                                "src/main/resources/data/leads_data.csv")
                                .ignoreFirstLine()
                                .variableNames("p_lastname", "p_company",
                                        "p_email_prefix", "p_leadsource",
                                        "p_amount"),
                        httpCache()
                                .disable(),
                        httpCookies()
                                .disable(),
                        // InfluxDB Backend Listener for real-time monitoring
                        influxDbListener("http://localhost:8086/api/v2/write?org=jmeter&bucket=jmeter&precision=ns")
                                .token("jmeter-admin-token-please-change-in-production")
                                .title("Salesforce Spike Test - Traffic Surge Validation")
                                .application("salesforce-spike-test"),
                        authGroup.getSetupThreadGroup(),
                        
                        // Phase 1: Baseline (2 users, gradual ramp)
                        leadToCashGroup.getLeadToCashThreadGroup(2, 10)
                                .rampTo(2, Duration.ofSeconds(30)),
                        
                        // Phase 2: Spike (sudden jump to 10 users)
                        leadToCashGroup.getLeadToCashThreadGroup(10, 5)
                                .rampTo(10, Duration.ofSeconds(10)),
                        
                        // Phase 3: Recovery (back to baseline)
                        leadToCashGroup.getLeadToCashThreadGroup(2, 10)
                                .rampTo(2, Duration.ofSeconds(30)),
                        
                        cleanUpGroup.getTeardownThreadGroup()
                );

    }
}
