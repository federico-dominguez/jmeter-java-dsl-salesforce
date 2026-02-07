package com.fedd.salesforce.plans;

import static us.abstracta.jmeter.javadsl.JmeterDsl.csvDataSet;
import static us.abstracta.jmeter.javadsl.JmeterDsl.httpCache;
import static us.abstracta.jmeter.javadsl.JmeterDsl.httpCookies;
import static us.abstracta.jmeter.javadsl.JmeterDsl.influxDbListener;
import static us.abstracta.jmeter.javadsl.JmeterDsl.testPlan;
import static us.abstracta.jmeter.javadsl.JmeterDsl.uniformRandomTimer;
import static us.abstracta.jmeter.javadsl.JmeterDsl.vars;

import com.fedd.salesforce.scenarios.AuthenticationSetupThreadGroup;
import com.fedd.salesforce.scenarios.CleanUpTeardownThreadGroup;
import com.fedd.salesforce.scenarios.LeadToCashThreadGroup;

import java.io.IOException;
import java.time.Duration;

import us.abstracta.jmeter.javadsl.core.DslTestPlan;

public class LeadToCashTestPlan {

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

                        // Random think time between 1-3 seconds
                        uniformRandomTimer(Duration.ofSeconds(1), Duration.ofSeconds(3)),
                                
                        // InfluxDB Backend Listener for real-time monitoring
                        influxDbListener("http://localhost:8086/api/v2/write?org=jmeter&bucket=jmeter&precision=ns")
                                .token("jmeter-admin-token-please-change-in-production")
                                .title("Salesforce Lead to Cash Performance Test")
                                .application("salesforce-lead-to-cash"),
                        authGroup.getSetupThreadGroup(),
                        leadToCashGroup.getLeadToCashThreadGroup(1, 1),
                        cleanUpGroup.getTeardownThreadGroup()
                );

    }
}
