package com.fedd.salesforce;

import static org.assertj.core.api.Assertions.assertThat;
import static us.abstracta.jmeter.javadsl.JmeterDsl.csvDataSet;
import static us.abstracta.jmeter.javadsl.JmeterDsl.httpCache;
import static us.abstracta.jmeter.javadsl.JmeterDsl.httpCookies;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jtlWriter;
import static us.abstracta.jmeter.javadsl.JmeterDsl.testPlan;
import static us.abstracta.jmeter.javadsl.JmeterDsl.vars;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import us.abstracta.jmeter.javadsl.core.DslTestPlan;
import us.abstracta.jmeter.javadsl.core.TestPlanStats;
import us.abstracta.jmeter.javadsl.core.listeners.DslViewResultsTree;

public class PerformanceTest {

        AuthenticationSetupThreadGroup authGroup = new AuthenticationSetupThreadGroup();
        LeadToCashThreadGroup leadToCashGroup = new LeadToCashThreadGroup();
        CleanUpTeardownThreadGroup cleanUpGroup = new CleanUpTeardownThreadGroup();

        @Test
        public void test() throws IOException {
                TestPlanStats stats = getTestPlan().children(
                                jtlWriter("target/jtls"), new DslViewResultsTree()).run();
                assertThat(stats.overall().errorsCount()).isEqualTo(0L);
        }

        @Test
        private DslTestPlan getTestPlan() throws IOException {

                return testPlan()
                                .sequentialThreadGroups()
                                .children(
                                                vars().set("BASE_URL",
                                                                "orgfarm-b8d4a27e18-dev-ed.develop.my.salesforce.com"),
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
                                                authGroup.getSetupThreadGroup(),
                                                leadToCashGroup.getLeadToCashThreadGroup(),
                                                cleanUpGroup.getTeardownThreadGroup());

        }

}