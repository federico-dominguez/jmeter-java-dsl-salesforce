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
 * Soak Test Plan - Long-Duration Stability
 * 
 * Purpose: Detect memory leaks, resource exhaustion, and performance degradation over time
 * 
 * Load Pattern:
 * - Ramp to 5 users over 5 minutes
 * - Hold at 5 users for 1 hour (sustained load)
 * 
 * Expected Behavior:
 * - Response times should remain stable throughout the test
 * - No gradual increase in response time (would indicate memory leak)
 * - Error rate should stay at 0% (no session expiry or resource exhaustion)
 * - Total API calls should stay well under daily limit (~1,500 calls for 1 hour)
 * 
 * Success Criteria:
 * - p95 response time at 55 minutes == p95 at 5 minutes (±10% acceptable)
 * - Error rate remains 0% throughout entire test
 * - No UNABLE_TO_LOCK_ROW errors (indicates database contention)
 * - Total API calls < 2,000 (leaves headroom for other testing)
 * 
 * What We're Testing For:
 * - Memory leaks (response time increases over time)
 * - Session management (OAuth token expiry, session leaks)
 * - Database connection pooling (connection leaks)
 * - Salesforce API daily limit accumulation
 * - Storage governor limits (5MB data storage)
 * 
 * Real-World Scenario:
 * - Simulates 8-hour business day compressed to 1 hour
 * - Tests system stability under continuous moderate load
 * - Validates that performance doesn't degrade during long sales cycles
 * 
 * Salesforce API Limits:
 * - Estimated API calls: ~1,500 (5 users × 1 hour × ~5 API calls/iteration)
 * - Well under daily limit of 5,000-15,000
 * - Concurrent requests: 5 (safe level, no throttling expected)
 * 
 * Monitoring Recommendations:
 * - Watch Grafana dashboard for response time trends
 * - Check InfluxDB for gradual increases in p95/p99 percentiles
 * - Monitor error rate over time (should be flat line at 0%)
 * - Verify throughput remains constant (no degradation)
 */
public class SoakTestPlan {

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
                                .title("Salesforce Soak Test - Long-Duration Stability")
                                .application("salesforce-soak-test"),
                        authGroup.getSetupThreadGroup(),
                        leadToCashGroup.getLeadToCashThreadGroup(5, 1)
                                .rampTo(5, Duration.ofMinutes(5))
                                .holdIterating("1h"), // Gentle ramp, then sustain for 1 hour
                        cleanUpGroup.getTeardownThreadGroup()
                );

    }
}
