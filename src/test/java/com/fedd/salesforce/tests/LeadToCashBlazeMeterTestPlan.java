package com.fedd.salesforce.tests;

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

public class LeadToCashBlazeMeterTestPlan {

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
                        leadToCashGroup.getLeadToCashThreadGroup(10,1)
                        ,cleanUpGroup.getTeardownThreadGroup()
                );

    }
}
