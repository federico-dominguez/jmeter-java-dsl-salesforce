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
 * Volume Test Plan - Large Dataset Performance
 * 
 * Purpose: Validate query performance and data processing with larger record counts
 * 
 * Load Pattern:
 * - 5 users processing 100 unique lead records
 * - Each lead goes through full Lead-to-Cash workflow
 * - Ramp to 5 users over 2 minutes
 * - Hold for 5 minutes or until all 100 records processed
 * 
 * Expected Behavior:
 * - Response times should remain stable regardless of dataset size
 * - SOQL queries should handle larger result sets efficiently
 * - Data creation time should scale linearly
 * - Storage governor limits should not be exceeded (100 leads × ~5KB = 500KB, well under 5MB limit)
 * 
 * Success Criteria:
 * - p95 response time with 100 records ≤ p95 with 10 records (±15% acceptable)
 * - Error rate remains 0% (no storage limit errors)
 * - All 100 unique leads created successfully
 * - No UNABLE_TO_LOCK_ROW errors (indicates good data distribution)
 * - Total storage used < 1MB (leaves headroom for other testing)
 * 
 * What We're Testing:
 * - SOQL query performance with larger result sets
 * - Data creation efficiency at scale
 * - Storage governor limit handling (5MB Developer Edition)
 * - CSV data handling (unique records, no duplicates)
 * - Cleanup performance (bulk delete operations)
 * 
 * Real-World Scenario:
 * - Simulates end-of-quarter data import (100 new leads)
 * - Tests batch processing performance
 * - Validates data integrity at higher volumes
 * - Ensures cleanup scripts can handle larger datasets
 * 
 * Salesforce Constraints:
 * - Developer Edition storage: 5MB (100 leads ≈ 500KB, well under limit)
 * - API calls: ~500 total (5 users × 100 records ÷ parallelism)
 * - Concurrent requests: 5 (safe level)
 * 
 * CSV Data Requirements:
 * - Must use volume_leads_data.csv (100 unique records)
 * - Ensures no duplicate email/company conflicts
 * - Tests with realistic variety of lead sources, amounts
 * 
 * Comparison Metrics:
 * - Compare against baseline test (10 records) to measure scaling impact
 * - Document response time increase per 100 records
 * - Calculate throughput (records processed per minute)
 */
public class VolumeTestPlan {

    private final AuthenticationSetupThreadGroup authGroup = new AuthenticationSetupThreadGroup();
    private final LeadToCashThreadGroup leadToCashGroup = new LeadToCashThreadGroup();
    private final CleanUpTeardownThreadGroup cleanUpGroup = new CleanUpTeardownThreadGroup();

    public DslTestPlan getTestPlan() throws IOException {
        return testPlan()
                .children(
                        vars().set("BASE_URL",
                                "orgfarm-b8d4a27e18-dev-ed.develop.my.salesforce.com")
                                .set("ownerId", "005gK00000AQ0ppQAD"),
                        // Use larger dataset for volume testing
                        csvDataSet(
                                "src/main/resources/data/volume_leads_data.csv")
                                .ignoreFirstLine()
                                .randomOrder() // Distribute load across different records
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
                                .title("Salesforce Volume Test - Large Dataset Performance")
                                .application("salesforce-volume-test"),
                        authGroup.getSetupThreadGroup(),
                        leadToCashGroup.getLeadToCashThreadGroup(5, 20) // 5 users × 20 iterations = 100 record attempts
                                .holdIterating("5m"),
                        cleanUpGroup.getTeardownThreadGroup()
                );

    }
}
