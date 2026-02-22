package com.fedd.salesforce.plans;

import static us.abstracta.jmeter.javadsl.JmeterDsl.csvDataSet;
import static us.abstracta.jmeter.javadsl.JmeterDsl.httpCache;
import static us.abstracta.jmeter.javadsl.JmeterDsl.httpCookies;
import static us.abstracta.jmeter.javadsl.JmeterDsl.influxDbListener;
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
 * Local execution test plan for the Lead-to-Cash workflow.
 * <p>
 * Runs with the embedded JMeter engine and sends metrics to InfluxDB
 * for real-time Grafana monitoring.
 * </p>
 */
public class LeadToCashTestPlan {

    private final AuthenticationSetupThreadGroup authGroup = new AuthenticationSetupThreadGroup();
    private final LeadToCashThreadGroup leadToCashGroup = new LeadToCashThreadGroup();
    private final CleanUpTeardownThreadGroup cleanUpGroup = new CleanUpTeardownThreadGroup();

    public DslTestPlan getTestPlan() throws IOException {
        return testPlan()
                .children(
                        vars().set("BASE_URL", TestConfig.BASE_URL)
                                .set("ownerId", TestConfig.OWNER_ID),
                        csvDataSet(TestConfig.CSV_PATH)
                                .ignoreFirstLine()
                                .variableNames("p_lastname", "p_company",
                                        "p_email_prefix", "p_leadsource",
                                        "p_amount"),
                        httpCache().disable(),
                        httpCookies().disable(),

                        // Random think time between 1-3 seconds
                        uniformRandomTimer(Duration.ofSeconds(1), Duration.ofSeconds(3)),

                        // InfluxDB Backend Listener for real-time monitoring
                        influxDbListener(TestConfig.INFLUXDB_URL)
                                .token(TestConfig.INFLUXDB_TOKEN)
                                .title("Salesforce Lead to Cash Performance Test")
                                .application("salesforce-lead-to-cash"),
                        authGroup.getSetupThreadGroup(),
                        leadToCashGroup.getLeadToCashThreadGroup(1, 1),
                        cleanUpGroup.getTeardownThreadGroup()
                );
    }

    /**
     * Debug variant: no think times, no InfluxDB listener.
     * <p>
     * Designed for quick validation runs where AI needs to read the full
     * sampler output. Use with {@code TestResultLogger} for structured reporting.
     * </p>
     */
    public DslTestPlan getDebugTestPlan() throws IOException {
        return testPlan()
                .children(
                        vars().set("BASE_URL", TestConfig.BASE_URL)
                                .set("ownerId", TestConfig.OWNER_ID),
                        csvDataSet(TestConfig.CSV_PATH)
                                .ignoreFirstLine()
                                .variableNames("p_lastname", "p_company",
                                        "p_email_prefix", "p_leadsource",
                                        "p_amount"),
                        httpCache().disable(),
                        httpCookies().disable(),
                        // No think time, no InfluxDB
                        authGroup.getSetupThreadGroup(),
                        leadToCashGroup.getLeadToCashThreadGroup(1, 1, false),
                        cleanUpGroup.getTeardownThreadGroup()
                );
    }
}
