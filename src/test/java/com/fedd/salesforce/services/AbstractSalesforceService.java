package com.fedd.salesforce.services;

import static us.abstracta.jmeter.javadsl.JmeterDsl.forEachController;
import static us.abstracta.jmeter.javadsl.JmeterDsl.httpSampler;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jsonAssertion;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jsonExtractor;
import static us.abstracta.jmeter.javadsl.JmeterDsl.responseAssertion;
import static us.abstracta.jmeter.javadsl.JmeterDsl.transaction;

import com.fedd.salesforce.config.TestConfig;

import org.apache.jmeter.protocol.http.util.HTTPConstants;

import us.abstracta.jmeter.javadsl.core.assertions.DslResponseAssertion.TargetField;
import us.abstracta.jmeter.javadsl.core.controllers.DslTransactionController;
import us.abstracta.jmeter.javadsl.core.postprocessors.DslJsonExtractor.JsonQueryLanguage;
import us.abstracta.jmeter.javadsl.http.DslHttpSampler;

/**
 * Abstract base class for Salesforce sObject service operations.
 * <p>
 * Provides reusable GET (by owner / all), DELETE, and bulk-delete operations
 * using {@link TestConfig} for centralized URL construction and API versioning.
 * Subclasses only need to define the sObject name, variable names, and any
 * custom create/update logic.
 * </p>
 *
 * @see TestConfig
 */
public abstract class AbstractSalesforceService {

    /**
     * Returns the Salesforce sObject API name (e.g. "Lead", "Account", "Task").
     */
    protected abstract String sObjectName();

    /**
     * Returns the JMeter variable name used to store extracted record IDs
     * (e.g. "leadId", "accountId").
     */
    protected abstract String idVariable();

    /**
     * Returns the JMeter variable name used inside ForEach controllers
     * for the current iteration's record ID (e.g. "currentLeadId").
     */
    protected abstract String currentIdVariable();

    /**
     * Returns a human-readable display name for this sObject
     * (e.g. "Lead", "Account"). Used in sampler and transaction names.
     */
    protected abstract String displayName();

    // ── GET Operations ──────────────────────────────────────────────────────

    /**
     * Fetches all record IDs for this sObject owned by the configured user.
     *
     * @return an HTTP sampler that queries records filtered by {@code ownerId}
     */
    public DslHttpSampler getByOwner() {
        return httpSampler("GET " + displayName() + "s",
                TestConfig.queryUrl("SELECT+Id+FROM+" + sObjectName() + "+WHERE+OwnerId='${ownerId}'"))
                .children(
                        jsonExtractor(idVariable(), "records[*].Id")
                                .matchNumber(-1)
                                .defaultValue(idVariable() + "_NOT_FOUND"),
                        jsonAssertion("Success Assertion", "$.done")
                                .queryLanguage(JsonQueryLanguage.JSON_PATH)
                                .equalsToJson("true"));
    }

    /**
     * Fetches all record IDs for this sObject regardless of owner.
     *
     * @return an HTTP sampler that queries all records
     */
    public DslHttpSampler getAll() {
        return httpSampler("GET All " + displayName() + "s",
                TestConfig.queryUrl("SELECT+Id+FROM+" + sObjectName()))
                .children(
                        jsonExtractor(idVariable(), "records[*].Id")
                                .matchNumber(-1)
                                .defaultValue(idVariable() + "_NOT_FOUND"),
                        jsonAssertion("Success Assertion", "$.done")
                                .queryLanguage(JsonQueryLanguage.JSON_PATH)
                                .equalsToJson("true"));
    }

    // ── DELETE Operations ───────────────────────────────────────────────────

    /**
     * Deletes a single record identified by the {@link #currentIdVariable()}.
     *
     * @return an HTTP sampler that sends a DELETE request and asserts HTTP 204
     */
    public DslHttpSampler deleteRecord() {
        return httpSampler("DELETE " + displayName(),
                TestConfig.recordUrl(sObjectName(), "${" + currentIdVariable() + "}"))
                .method(HTTPConstants.DELETE)
                .children(
                        responseAssertion()
                                .fieldToTest(TargetField.RESPONSE_CODE)
                                .equalsToStrings("204"));
    }

    /**
     * Retrieves all records owned by the user and deletes them in a loop.
     *
     * @return a transaction controller wrapping a GET + ForEach DELETE
     */
    public DslTransactionController deleteAll() {
        return transaction(displayName() + "s Clean Up",
                getByOwner(),
                forEachController("ForEach " + displayName() + "Id",
                        idVariable(),
                        currentIdVariable(),
                        deleteRecord()));
    }
}
