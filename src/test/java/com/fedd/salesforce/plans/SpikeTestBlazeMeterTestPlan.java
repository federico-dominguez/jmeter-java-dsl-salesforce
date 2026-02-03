package com.fedd.salesforce.plans;

import static us.abstracta.jmeter.javadsl.JmeterDsl.csvDataSet;
import static us.abstracta.jmeter.javadsl.JmeterDsl.httpCache;
import static us.abstracta.jmeter.javadsl.JmeterDsl.httpCookies;
import static us.abstracta.jmeter.javadsl.JmeterDsl.testPlan;
import static us.abstracta.jmeter.javadsl.JmeterDsl.vars;

import com.fedd.salesforce.scenarios.AuthenticationSetupThreadGroup;
import com.fedd.salesforce.scenarios.CleanUpTeardownThreadGroup;
import com.fedd.salesforce.scenarios.LeadToCashThreadGroup;

import java.io.IOException;

import us.abstracta.jmeter.javadsl.core.DslTestPlan;

/**
 * Spike Test Plan - BlazeMeter Cloud Execution
 * 
 * Purpose: Test system behavior during sudden traffic spikes on BlazeMeter's cloud infrastructure.
 * 
 * Load Pattern (3 phases):
 * - Phase 1 (Baseline): 2 users, 3 iterations each = 6 workflows
 * - Phase 2 (Spike): 10 users, 2 iterations each = 20 workflows (sudden surge)
 * - Phase 3 (Recovery): 2 users, 3 iterations each = 6 workflows
 * Total: 32 workflows × 7 records/workflow = 224 records (fits within 5MB Dev Edition limit)
 * 
 * BlazeMeter Configuration:
 * - Uses asset reference: leads_data.csv (must be uploaded to BlazeMeter)
 * - Credentials passed as environment variables or BlazeMeter test configuration
 * - Total threads: 14 across 3 sequential thread groups
 * - Duration: ~5-7 minutes
 * 
 * Expected Behavior:
 * - Baseline phase establishes normal performance
 * - Spike phase triggers Salesforce rate limiting
 * - Recovery phase validates system returns to normal
 * 
 * Success Criteria:
 * - Error rate during spike < 20%
 * - Recovery time < 2 minutes
 * - No errors during recovery phase
 * 
 * BlazeMeter Setup Required:
 * 1. Upload leads_data.csv as test asset
 * 2. Set environment variables in BlazeMeter UI:
 *    - SALESFORCE_USERNAME
 *    - SALESFORCE_CLIENT_ID  
 *    - SALESFORCE_PRIVATE_KEY
 *    - AUDIENCE
 */
public class SpikeTestBlazeMeterTestPlan {

    private final AuthenticationSetupThreadGroup authGroup = new AuthenticationSetupThreadGroup();
    private final LeadToCashThreadGroup leadToCashGroup = new LeadToCashThreadGroup();
    private final CleanUpTeardownThreadGroup cleanUpGroup = new CleanUpTeardownThreadGroup();

    public DslTestPlan getTestPlan() throws IOException {
        // Read credentials from environment variables (for local/CI) or fall back to system properties
        String username = System.getenv("SALESFORCE_USERNAME");
        String clientId = System.getenv("SALESFORCE_CLIENT_ID");
        String privateKey = System.getenv("SALESFORCE_PRIVATE_KEY");
        String audience = System.getenv("AUDIENCE");
        
        if (audience == null || audience.isEmpty()) {
            audience = "https://orgfarm-b8d4a27e18-dev-ed.develop.my.salesforce.com";
        }

        return testPlan()
                .children(
                        vars().set("BASE_URL",
                                "orgfarm-b8d4a27e18-dev-ed.develop.my.salesforce.com")
                                .set("ownerId", "005gK00000AQ0ppQAD")
                                // Set credentials as JMeter variables for BlazeMeter execution
                                .set("SALESFORCE_USERNAME", username != null ? username : "EDIT_IN_BLAZEMETER_UI")
                                .set("SALESFORCE_CLIENT_ID", clientId != null ? clientId : "EDIT_IN_BLAZEMETER_UI")
                                .set("SALESFORCE_PRIVATE_KEY", privateKey != null ? privateKey : "EDIT_IN_BLAZEMETER_UI")
                                .set("AUDIENCE", audience),
                        // Reference asset filename (uploaded to BlazeMeter)
                        csvDataSet(
                                "leads_data.csv")
                                .ignoreFirstLine()
                                .variableNames("p_lastname", "p_company",
                                        "p_email_prefix", "p_leadsource",
                                        "p_amount"),
                        httpCache()
                                .disable(),
                        httpCookies()
                                .disable(),
                        authGroup.getSetupThreadGroup(),
                        
                        // Phase 1: Baseline (2 users, 3 iterations each = 6 workflows)
                        leadToCashGroup.getLeadToCashThreadGroup(2, 3),
                        
                        // Phase 2: Spike (sudden jump to 10 users, 2 iterations each = 20 workflows)
                        leadToCashGroup.getLeadToCashThreadGroup(10, 2),
                        
                        // Phase 3: Recovery (back to 2 users, 3 iterations each = 6 workflows)
                        leadToCashGroup.getLeadToCashThreadGroup(2, 3),
                        
                        // Total: 32 workflows × 7 records = 224 records (well under 5MB limit)
                        cleanUpGroup.getTeardownThreadGroup()
                );

    }
}
