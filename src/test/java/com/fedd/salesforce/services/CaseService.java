package com.fedd.salesforce.services;

import static us.abstracta.jmeter.javadsl.JmeterDsl.httpSampler;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jsonAssertion;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jsonExtractor;

import com.fedd.salesforce.config.TestConfig;

import org.apache.http.entity.ContentType;

import us.abstracta.jmeter.javadsl.core.controllers.DslTransactionController;
import us.abstracta.jmeter.javadsl.core.postprocessors.DslJsonExtractor.JsonQueryLanguage;
import us.abstracta.jmeter.javadsl.http.DslHttpSampler;

/**
 * Service class for creating, querying, and deleting Salesforce Case records
 * using the JMeter Java DSL.
 *
 * <p>Inherits standard GET, DELETE, and bulk-delete operations from
 * {@link AbstractSalesforceService}.</p>
 */
public class CaseService extends AbstractSalesforceService {

        @Override protected String sObjectName()       { return "Case"; }
        @Override protected String idVariable()         { return "caseId"; }
        @Override protected String currentIdVariable()  { return "currentCaseId"; }
        @Override protected String displayName()        { return "Case"; }

        /** @deprecated Use {@link #getByOwner()} instead. */
        public DslHttpSampler getCases() { return getByOwner(); }

        /** @deprecated Use {@link #getAll()} instead. */
        public DslHttpSampler getAllCases() { return getAll(); }

        /** @deprecated Use {@link #deleteRecord()} instead. */
        public DslHttpSampler deleteCase() { return deleteRecord(); }

        /**
         * Creates a new Case record in Salesforce.
         * <p>Uses the contactId from the Lead conversion (stored in JMeter properties)
         * instead of a hardcoded value.</p>
         *
         * @return an HTTP sampler that POSTs a Case and validates the response
         */
        public DslHttpSampler createCase() {
                return httpSampler("CREATE New Case", TestConfig.restUrl("Case"))
                                .post("""
                                                {
                                                 "ContactId": "${__P(CONTACT_ID,)}",
                                                 "OwnerId": "${ownerId}",
                                                 "Priority": "Normal",
                                                 "Reason": "Performance",
                                                 "Status": "New",
                                                 "Subject": "Performance inadequate for second consecutive week",
                                                 "Type" : "Electrical"
                                                }
                                                """,
                                                ContentType.APPLICATION_JSON)
                                .children(
                                                jsonExtractor("caseId", "id")
                                                                .defaultValue("caseId_NOT_FOUND"),
                                                jsonAssertion("Success Assertion", "$.success")
                                                                .queryLanguage(JsonQueryLanguage.JSON_PATH)
                                                                .equalsToJson("true"));
        }

        /** @deprecated Use {@link #deleteAll()} instead. */
        public DslTransactionController deleteAllCases() { return deleteAll(); }
}
