package com.fedd.salesforce;

import static org.assertj.core.api.Assertions.assertThat;
import static us.abstracta.jmeter.javadsl.JmeterDsl.csvDataSet;
import static us.abstracta.jmeter.javadsl.JmeterDsl.httpCache;
import static us.abstracta.jmeter.javadsl.JmeterDsl.httpCookies;
import static us.abstracta.jmeter.javadsl.JmeterDsl.httpHeaders;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jtlWriter;
import static us.abstracta.jmeter.javadsl.JmeterDsl.testPlan;
import static us.abstracta.jmeter.javadsl.JmeterDsl.threadGroup;
import static us.abstracta.jmeter.javadsl.JmeterDsl.vars;
import com.fedd.salesforce.scenarios.CleanUpTeardownThreadGroup;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.w3c.tidy.Clean;

import com.fedd.salesforce.scenarios.AuthenticationSetupThreadGroup;
import com.fedd.salesforce.services.LeadService;
import com.fedd.salesforce.services.NoteService;

import us.abstracta.jmeter.javadsl.core.DslTestPlan;
import us.abstracta.jmeter.javadsl.core.TestPlanStats;
import us.abstracta.jmeter.javadsl.core.listeners.DslViewResultsTree;

public class TestServices {

    private final LeadService leadService = new LeadService();
    private final NoteService noteService = new NoteService();
    private final AuthenticationSetupThreadGroup authGroup = new AuthenticationSetupThreadGroup();
    private final CleanUpTeardownThreadGroup cleanUpGroup = new CleanUpTeardownThreadGroup();

    @Test
    public void test() throws IOException {
        TestPlanStats stats = getTestPlan().children(new DslViewResultsTree()).run();
        assertThat(stats.overall().errorsCount()).isEqualTo(0L);
    }

    private DslTestPlan getTestPlan() throws IOException {
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
                        authGroup.getSetupThreadGroup(),
                        threadGroup("Test Services Thread Group", 1, 1)
                                .children(
                                        httpHeaders()
                                                .header("Authorization",
                                                        "Bearer ${__P(ACCESS_TOKEN,)}"),
                                        leadService.createLead(),
                                        noteService.createNote()),
                                    cleanUpGroup.getTeardownThreadGroup());

    }
}
