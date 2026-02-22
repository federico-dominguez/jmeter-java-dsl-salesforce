package com.fedd.salesforce.utils;

import org.apache.jmeter.samplers.SampleResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AI-friendly test result logger that writes a structured plain-text report.
 * <p>
 * Captures complete sampler results (names, HTTP codes, durations, URLs,
 * request/response bodies, assertion failures) in a format optimised for
 * both human review and AI parsing from console or file output.
 * </p>
 * <p>
 * Usage inside a debug test:
 * <pre>
 * TestResultLogger.init();
 * TestPlanStats stats = testPlan.children(
 *     jsr223PostProcessor(s -&gt; TestResultLogger.logSample(s.prev))
 * ).run();
 * TestResultLogger.printReport();
 * </pre>
 * The report is written to {@value #REPORT_FILE}.
 * </p>
 */
public final class TestResultLogger {

    /** Relative path where the debug report is written. */
    public static final String REPORT_FILE = "target/debug-report.txt";
    private static final Path REPORT_PATH = Path.of(REPORT_FILE);
    private static final AtomicInteger COUNTER = new AtomicInteger(0);
    private static final int MAX_BODY_LENGTH = 2000;

    private TestResultLogger() {
        // Utility class
    }

    // ── Lifecycle ───────────────────────────────────────────────────────────

    /**
     * Resets the counter and creates a fresh report file with a header.
     * Call once <b>before</b> the test plan executes.
     */
    public static void init() {
        COUNTER.set(0);
        try {
            Files.deleteIfExists(REPORT_PATH);
            String header =
                    "================================================================================\n"
                  + "                    PERFORMANCE TEST DEBUG REPORT\n"
                  + "================================================================================\n"
                  + "  Format: #N [STATUS] HTTP_CODE SAMPLER_NAME | DURATIONms\n"
                  + "================================================================================\n\n";
            Files.writeString(REPORT_PATH, header, StandardOpenOption.CREATE);
        } catch (IOException e) {
            System.err.println("[TestResultLogger] init failed: " + e.getMessage());
        }
    }

    /**
     * Appends a summary footer and prints the full report to stdout.
     * Call <b>after</b> the test plan finishes so the console output is
     * captured in terminal / CI logs for AI analysis.
     */
    public static void printReport() {
        try {
            String footer = String.format(
                    "\n================================================================================\n"
                  + "  Total Samplers Logged: %d\n"
                  + "================================================================================\n",
                    COUNTER.get());
            Files.writeString(REPORT_PATH, footer, StandardOpenOption.APPEND);

            // Print full report to stdout (AI reads from terminal output)
            System.out.println(Files.readString(REPORT_PATH));
        } catch (IOException e) {
            System.err.println("[TestResultLogger] printReport failed: " + e.getMessage());
        }
    }

    // ── Per-sampler logging ─────────────────────────────────────────────────

    /**
     * Logs one sampler result to the report file.
     * Thread-safe — can be called from concurrent JMeter threads.
     *
     * @param sample the {@link SampleResult} obtained from the post-processor
     */
    public static synchronized void logSample(SampleResult sample) {
        int n = COUNTER.incrementAndGet();
        StringBuilder sb = new StringBuilder();

        String status = sample.isSuccessful() ? "PASS" : "FAIL";

        // ── Header line ─────────────────────────────────────────────────────
        sb.append(String.format("#%-3d [%s] %s %s | %dms%n",
                n, status, sample.getResponseCode(),
                sample.getSampleLabel(), sample.getTime()));

        // ── URL ─────────────────────────────────────────────────────────────
        String url = sample.getUrlAsString();
        if (url != null && !url.isEmpty()) {
            sb.append(String.format("     URL: %s%n", url));
        }

        // ── Request data (method + URL + body for HTTP samplers) ────────────
        String reqData = sample.getSamplerData();
        if (reqData != null && !reqData.isBlank()) {
            sb.append(String.format("     Request: %s%n", truncate(reqData)));
        }

        // ── Response body ───────────────────────────────────────────────────
        String resData = sample.getResponseDataAsString();
        if (resData != null && !resData.isBlank()) {
            sb.append(String.format("     Response (%d bytes): %s%n",
                    sample.getBytesAsLong(), truncate(resData)));
        }

        // ── Assertion failures ──────────────────────────────────────────────
        if (sample.getAssertionResults() != null) {
            for (var ar : sample.getAssertionResults()) {
                if (ar.isFailure() || ar.isError()) {
                    sb.append(String.format("     >> ASSERTION FAILED: %s - %s%n",
                            ar.getName(), ar.getFailureMessage()));
                }
            }
        }

        // ── Sub-results (redirects, embedded resources) ─────────────────────
        SampleResult[] subResults = sample.getSubResults();
        if (subResults != null && subResults.length > 0) {
            for (SampleResult sub : subResults) {
                sb.append(String.format("     [sub] [%s] %s %s | %dms%n",
                        sub.isSuccessful() ? "PASS" : "FAIL",
                        sub.getResponseCode(),
                        sub.getSampleLabel(),
                        sub.getTime()));
            }
        }

        sb.append("--------------------------------------------------------------------------------\n");

        try {
            Files.writeString(REPORT_PATH, sb.toString(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("[TestResultLogger] write failed: " + e.getMessage());
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private static String truncate(String text) {
        if (text == null) return "";
        // Flatten newlines for compact single-line display
        text = text.replace("\r\n", " ").replace("\n", " ").replace("\r", " ").trim();
        if (text.length() <= MAX_BODY_LENGTH) return text;
        return text.substring(0, MAX_BODY_LENGTH)
                + "... [" + text.length() + " chars total]";
    }
}
