package com.fedd.salesforce;

import static org.assertj.core.api.Assertions.assertThat;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jtlWriter;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fedd.salesforce.tests.LeadToCashTestPlan;
import com.fedd.salesforce.tests.TestServiceTestPlan;

import us.abstracta.jmeter.javadsl.core.TestPlanStats;
import us.abstracta.jmeter.javadsl.core.listeners.DslViewResultsTree;

public class PerformanceTest {

        private final LeadToCashTestPlan leadToCashTestPlan = new LeadToCashTestPlan();
        private final TestServiceTestPlan testServiceTestPlan = new TestServiceTestPlan();

        @Test
        public void test() throws IOException {
                TestPlanStats stats = leadToCashTestPlan.getTestPlan().children(
                                jtlWriter("target/jtls"), new DslViewResultsTree()).run();
                assertThat(stats.overall().errorsCount()).isEqualTo(0L);
        }

        @Test
        public void testServices() throws IOException {
                TestPlanStats stats = testServiceTestPlan.getTestPlan().children(new DslViewResultsTree()).run();
                assertThat(stats.overall().errorsCount()).isEqualTo(0L);
        }

}