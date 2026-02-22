package com.fedd.salesforce.plans;

import static us.abstracta.jmeter.javadsl.JmeterDsl.csvDataSet;
import static us.abstracta.jmeter.javadsl.JmeterDsl.httpCache;
import static us.abstracta.jmeter.javadsl.JmeterDsl.httpCookies;
import static us.abstracta.jmeter.javadsl.JmeterDsl.testPlan;
import static us.abstracta.jmeter.javadsl.JmeterDsl.uniformRandomTimer;
import static us.abstracta.jmeter.javadsl.JmeterDsl.vars;

import com.fedd.salesforce.config.TestConfig;
import com.fedd.salesforce.scenarios.AuthenticationSetupThreadGroup;
import com.fedd.salesforce.scenarios.CleanUpTeardownThreadGroup;
import com.fedd.salesforce.scenarios.LeadToCashThreadGroup;

import java.io.IOException;
import java.time.Duration;

import us.abstracta.jmeter.javadsl.core.DslTestPlan;

/**
 * BlazeMeter cloud execution test plan for the Lead-to-Cash workflow.
 * <p>
 * Reads Salesforce credentials from environment variables and passes them
 * as JMeter variables for the BlazeMeter engine. Includes think time for
 * realistic load simulation.
 * </p>
 */
public class LeadToCashBlazeMeterTestPlan {

    private final AuthenticationSetupThreadGroup authGroup = new AuthenticationSetupThreadGroup();
    private final LeadToCashThreadGroup leadToCashGroup = new LeadToCashThreadGroup();
    private final CleanUpTeardownThreadGroup cleanUpGroup = new CleanUpTeardownThreadGroup();

    public DslTestPlan getTestPlan() throws IOException {
        return testPlan()
                .children(
                        vars().set("BASE_URL", TestConfig.BASE_URL)
                                .set("ownerId", TestConfig.OWNER_ID)
                                // Set credentials as JMeter variables for BlazeMeter execution
                                .set("SALESFORCE_USERNAME",
                                        TestConfig.USERNAME != null ? TestConfig.USERNAME : "EDIT_IN_BLAZEMETER_UI")
                                .set("SALESFORCE_CLIENT_ID",
                                        TestConfig.CLIENT_ID != null ? TestConfig.CLIENT_ID : "EDIT_IN_BLAZEMETER_UI")
                                .set("SALESFORCE_PRIVATE_KEY",
                                        TestConfig.PRIVATE_KEY != null ? TestConfig.PRIVATE_KEY : "EDIT_IN_BLAZEMETER_UI")
                                .set("AUDIENCE", TestConfig.AUDIENCE),
                        csvDataSet(TestConfig.CSV_ASSET_FILENAME)
                                .ignoreFirstLine()
                                .variableNames("p_lastname", "p_company",
                                        "p_email_prefix", "p_leadsource",
                                        "p_amount"),
                        httpCache().disable(),
                        httpCookies().disable(),

                        // Random think time between 1-3 seconds (consistent with local plan)
                        uniformRandomTimer(Duration.ofSeconds(1), Duration.ofSeconds(3)),

                        authGroup.getSetupThreadGroup(),
                        leadToCashGroup.getLeadToCashThreadGroup(10, 1),
                        cleanUpGroup.getTeardownThreadGroup()
                );
    }
}
