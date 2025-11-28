package com.fedd.salesforce;

import static org.assertj.core.api.Assertions.assertThat;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jtlWriter;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fedd.salesforce.tests.LeadToCashTestPlan;

import us.abstracta.jmeter.javadsl.core.TestPlanStats;
import us.abstracta.jmeter.javadsl.core.listeners.DslViewResultsTree;

public class PerformanceTest {

        private final LeadToCashTestPlan leadToCashTestPlan = new LeadToCashTestPlan();

        @Test
        public void test() throws IOException {
                TestPlanStats stats = leadToCashTestPlan.getTestPlan().children(
                                jtlWriter("target/jtls"), new DslViewResultsTree()).run();
                assertThat(stats.overall().errorsCount()).isEqualTo(0L);
        }

}