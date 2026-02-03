package com.fedd.salesforce.plans;

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
 * Stress Test Plan - Breaking Point Analysis
 * 
 * Purpose: Gradually increase load to find Salesforce Developer Edition's actual concurrent request limit
 * 
 * Load Pattern:
 * - Ramp from 1 to 15 users over 10 minutes
 * - Hold at 15 users for 20 iterations each
 * 
 * Expected Behavior:
 * - Should see throttling/errors around 10-15 concurrent users (Salesforce Dev limit)
 * - Response times should increase as we approach the limit
 * - May see HTTP 503 (Service Unavailable) or UNABLE_TO_LOCK_ROW errors
 * 
 * Success Criteria:
 * - Identify the exact user count where p95 response time exceeds 5 seconds
 * - Document when errors start appearing (error rate > 0%)
 * - Determine stable concurrent user capacity for this environment
 * 
 * Salesforce API Limits:
 * - Concurrent requests: 5-25 (typically ~10-15 for Developer Edition)
 * - Daily API calls: 5,000-15,000
 * - Estimated API calls for this test: ~300-450 (well under limit)
 */
public class StressTestPlan {

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
                                .title("Salesforce Stress Test - Breaking Point Analysis")
                                .application("salesforce-stress-test"),
                        authGroup.getSetupThreadGroup(),
                        leadToCashGroup.getLeadToCashThreadGroup(15, 20), // 15 users, 20 iterations each
                        cleanUpGroup.getTeardownThreadGroup()
                );

    }
}
