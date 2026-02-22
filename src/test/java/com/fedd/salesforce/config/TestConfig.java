package com.fedd.salesforce.config;

/**
 * Centralized configuration for all test plans and services.
 * <p>
 * Reads values from environment variables with sensible defaults.
 * Eliminates hardcoded URLs, IDs, and tokens scattered across the codebase.
 * </p>
 */
public final class TestConfig {

    private TestConfig() {
        // Utility class — not instantiable
    }

    // ── Salesforce API ──────────────────────────────────────────────────────

    /** Salesforce API version used across all REST and SOAP endpoints. */
    public static final String API_VERSION = "v60.0";

    /** SOAP API version number (without the 'v' prefix). */
    public static final String SOAP_API_VERSION = "60.0";

    /** Salesforce instance base URL (without protocol). */
    public static final String BASE_URL = env("SALESFORCE_BASE_URL",
            "orgfarm-b8d4a27e18-dev-ed.develop.my.salesforce.com");

    /** Salesforce user ID used for record ownership and SOQL filters. */
    public static final String OWNER_ID = env("OWNER_ID", "005gK00000AQ0ppQAD");

    /** Salesforce login audience URL for JWT Bearer Flow. */
    public static final String AUDIENCE = env("AUDIENCE",
            "https://orgfarm-b8d4a27e18-dev-ed.develop.my.salesforce.com");

    // ── Salesforce Credentials (environment-only, no defaults) ──────────────

    /** Salesforce username for JWT authentication. */
    public static final String USERNAME = env("SALESFORCE_USERNAME", null);

    /** Connected App consumer key. */
    public static final String CLIENT_ID = env("SALESFORCE_CLIENT_ID", null);

    /** Base64-encoded RSA private key. */
    public static final String PRIVATE_KEY = env("SALESFORCE_PRIVATE_KEY", null);

    // ── InfluxDB / Monitoring ───────────────────────────────────────────────

    /** InfluxDB write endpoint for the JMeter backend listener. */
    public static final String INFLUXDB_URL = env("INFLUXDB_URL",
            "http://localhost:8086/api/v2/write?org=jmeter&bucket=jmeter&precision=ns");

    /** InfluxDB authentication token. */
    public static final String INFLUXDB_TOKEN = env("INFLUXDB_TOKEN",
            "jmeter-admin-token-please-change-in-production");

    // ── BlazeMeter ──────────────────────────────────────────────────────────

    /** BlazeMeter API token ({key}:{secret}). */
    public static final String BZ_TOKEN = env("BZ_TOKEN", null);

    // ── Test Data ───────────────────────────────────────────────────────────

    /** Path to the CSV test data file (local execution). */
    public static final String CSV_PATH = "src/main/resources/data/leads_data.csv";

    /** CSV filename used for BlazeMeter asset uploads. */
    public static final String CSV_ASSET_FILENAME = "leads_data.csv";

    // ── URL Helpers ─────────────────────────────────────────────────────────

    /**
     * Builds a Salesforce REST API URL path for the given sObject.
     *
     * @param sObjectName the Salesforce object name (e.g. "Lead", "Account")
     * @return the full URL with JMeter variable interpolation for BASE_URL
     */
    public static String restUrl(String sObjectName) {
        return "https://${BASE_URL}/services/data/" + API_VERSION + "/sobjects/" + sObjectName + "/";
    }

    /**
     * Builds a Salesforce SOQL query URL.
     *
     * @param soql the SOQL query string (URL-encoded)
     * @return the full query URL with JMeter variable interpolation for BASE_URL
     */
    public static String queryUrl(String soql) {
        return "https://${BASE_URL}/services/data/" + API_VERSION + "/query/?q=" + soql;
    }

    /**
     * Builds a Salesforce REST API URL for a specific record.
     *
     * @param sObjectName the Salesforce object name
     * @param idVariable  the JMeter variable name holding the record ID (e.g. "${currentLeadId}")
     * @return the full URL for the record
     */
    public static String recordUrl(String sObjectName, String idVariable) {
        return "https://${BASE_URL}/services/data/" + API_VERSION + "/sobjects/" + sObjectName + "/" + idVariable;
    }

    /**
     * Returns the Salesforce SOAP API endpoint URL.
     *
     * @return the SOAP endpoint URL with JMeter variable interpolation
     */
    public static String soapUrl() {
        return "https://${BASE_URL}/services/Soap/c/" + SOAP_API_VERSION;
    }

    /**
     * Returns the Salesforce OAuth2 token endpoint URL.
     *
     * @return the OAuth2 token URL with JMeter variable interpolation
     */
    public static String authUrl() {
        return "https://${BASE_URL}/services/oauth2/token";
    }

    // ── Internal ────────────────────────────────────────────────────────────

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }
}
