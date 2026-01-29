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

                // BlazeMeter engine expects assets to be accessible by filename from working directory
                // Copy CSV to project root temporarily for upload
                java.io.File sourceFile = new java.io.File("src/main/resources/data/leads_data.csv");
                java.io.File dataFile = new java.io.File("leads_data.csv");
                java.nio.file.Files.copy(sourceFile.toPath(), dataFile.toPath(), 
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);

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