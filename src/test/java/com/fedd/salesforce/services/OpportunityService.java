package com.fedd.salesforce.services;

import static us.abstracta.jmeter.javadsl.JmeterDsl.httpSampler;
import static us.abstracta.jmeter.javadsl.JmeterDsl.responseAssertion;

import com.fedd.salesforce.config.TestConfig;

import org.apache.http.entity.ContentType;
import org.apache.jmeter.protocol.http.util.HTTPConstants;

import us.abstracta.jmeter.javadsl.core.assertions.DslResponseAssertion.TargetField;
import us.abstracta.jmeter.javadsl.core.controllers.DslTransactionController;
import us.abstracta.jmeter.javadsl.http.DslHttpSampler;

/**
 * Service class for querying, updating, and deleting Salesforce Opportunity records
 * using the JMeter Java DSL.
 *
 * <p>Opportunities are created implicitly via Lead conversion. This service provides
 * the close operation (update StageName to "Closed Won") and cleanup.
 * Inherits standard GET, DELETE, and bulk-delete from {@link AbstractSalesforceService}.</p>
 */
public class OpportunityService extends AbstractSalesforceService {

        @Override protected String sObjectName()       { return "Opportunity"; }
        @Override protected String idVariable()         { return "opportunityId"; }
        @Override protected String currentIdVariable()  { return "currentOpportunityId"; }
        @Override protected String displayName()        { return "Opportunity"; }

        /** @deprecated Use {@link #getByOwner()} instead. */
        public DslHttpSampler getOpportunities() { return getByOwner(); }

        /** @deprecated Use {@link #getAll()} instead. */
        public DslHttpSampler getAllOpportunities() { return getAll(); }

        /** @deprecated Use {@link #deleteRecord()} instead. */
        public DslHttpSampler deleteOpportunity() { return deleteRecord(); }

        /**
         * Updates an Opportunity to "Closed Won" with the parameterized amount.
         *
         * @return an HTTP sampler that PATCHes the Opportunity and asserts HTTP 204
         */
        public DslHttpSampler closeOpportunity() {
                return httpSampler("UPDATE Opportunity to Closed Won",
                                TestConfig.recordUrl("Opportunity", "${opportunityId}"))
                                .method(HTTPConstants.PATCH)
                                .contentType(ContentType.APPLICATION_JSON)
                                .body("""
                                                {
                                                 "StageName": "Closed Won",
                                                 "Amount": ${p_amount}
                                                }
                                                """)
                                .children(
                                                responseAssertion("Response Code Assertion")
                                                                .fieldToTest(TargetField.RESPONSE_CODE)
                                                                .equalsToStrings("204"));
        }

        /** @deprecated Use {@link #deleteAll()} instead. */
        public DslTransactionController deleteAllOpportunities() { return deleteAll(); }
}
