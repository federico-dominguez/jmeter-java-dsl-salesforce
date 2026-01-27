package com.fedd.salesforce;

import static org.assertj.core.api.Assertions.assertThat;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jtlWriter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

import com.fedd.salesforce.tests.LeadToCashTestPlan;
import com.fedd.salesforce.tests.LeadToCashBlazeMeterTestPlan;

import us.abstracta.jmeter.javadsl.blazemeter.BlazeMeterEngine;
import us.abstracta.jmeter.javadsl.core.TestPlanStats;
import us.abstracta.jmeter.javadsl.core.listeners.DslViewResultsTree;

public class PerformanceTest {

        private final static LeadToCashTestPlan leadToCashTestPlan = new LeadToCashTestPlan();
        private final static LeadToCashBlazeMeterTestPlan leadToCashBlazePlan = new LeadToCashBlazeMeterTestPlan();

        public static void main(String[] args) throws IOException, InterruptedException, TimeoutException {
                blazeMeterTest();
        }

        @Test
        public void test() throws IOException {
                TestPlanStats stats = leadToCashTestPlan.getTestPlan().children(
                                jtlWriter("target/jtls"), new DslViewResultsTree()).run();
                assertThat(stats.overall().errorsCount()).isEqualTo(0L);
        }

        @Test
        public static void blazeMeterTest() throws IOException, InterruptedException, TimeoutException {
                String bzToken = System.getenv("BZ_TOKEN");
                if (bzToken == null || bzToken.isEmpty()) {
                        System.out.println("BZ_TOKEN not set; skipping BlazeMeter test run.");
                        return;
                }

                // When running in BlazeMeter, upload CSV as an asset named 'leads_data.csv'.
                final String FILE_PATH = "leads_data.csv";
                java.io.File dataFile = new java.io.File(FILE_PATH);

                // Use the BlazeMeter-specific test plan that references the CSV asset filename.
                TestPlanStats stats = leadToCashBlazePlan.getTestPlan()
                                .runIn(new BlazeMeterEngine(bzToken)
                                                .testName("Salesforce Lead to Cash Performance Test")
                                                .totalUsers(1)
                                                .rampUpFor(Duration.ofMinutes(1))
                                                .holdFor(Duration.ofMinutes(1))
                                                .testTimeout(Duration.ofMinutes(1))
                                                .assets(dataFile));
                assertThat(stats.overall().sampleTimePercentile99()).isLessThan(Duration.ofSeconds(5));
        }

}