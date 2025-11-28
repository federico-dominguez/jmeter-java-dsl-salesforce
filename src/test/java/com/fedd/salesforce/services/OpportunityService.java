package com.fedd.salesforce.services;

import static us.abstracta.jmeter.javadsl.JmeterDsl.forEachController;
import static us.abstracta.jmeter.javadsl.JmeterDsl.httpSampler;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jsonAssertion;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jsonExtractor;
import static us.abstracta.jmeter.javadsl.JmeterDsl.responseAssertion;
import static us.abstracta.jmeter.javadsl.JmeterDsl.transaction;

import org.apache.http.entity.ContentType;
import org.apache.jmeter.protocol.http.util.HTTPConstants;

import us.abstracta.jmeter.javadsl.core.assertions.DslResponseAssertion.TargetField;
import us.abstracta.jmeter.javadsl.core.controllers.DslTransactionController;
import us.abstracta.jmeter.javadsl.core.postprocessors.DslJsonExtractor.JsonQueryLanguage;
import us.abstracta.jmeter.javadsl.http.DslHttpSampler;

public class OpportunityService {
        public DslHttpSampler getOpportunities() {
                return httpSampler("GET Opportunities",
                                "https://${BASE_URL}/services/data/v60.0/query/?q=SELECT+Id+FROM+Opportunity+WHERE+OwnerId='${ownerId}'")
                                .children(
                                                jsonExtractor("opportunityId",
                                                                "records[*].Id")
                                                                .matchNumber(-1)
                                                                .defaultValue("opportunityId_NOT_FOUND"),
                                                jsonAssertion("Success Assertion",
                                                                "$.done")
                                                                .queryLanguage(JsonQueryLanguage.JSON_PATH)
                                                                .equalsToJson("true"));
        }

        public DslHttpSampler closeOpportunity() {
                return httpSampler("UPDATE Opportunity to Closed Won",
                                "https://${BASE_URL}/services/data/v60.0/sobjects/Opportunity/${opportunityId}")
                                .method(HTTPConstants.PATCH)
                                .contentType(ContentType.APPLICATION_JSON)
                                .body("""
                                                {
                                                 "StageName": "Closed Won",
                                                 "Amount": ${p_amount}
                                                }
                                                """)
                                .children(
                                                responseAssertion(
                                                                "Response Code Assertion")
                                                                .fieldToTest(TargetField.RESPONSE_CODE)
                                                                .equalsToStrings(
                                                                                "204"));
        }

        public DslHttpSampler deleteOpportunity() {
                return httpSampler("DELETE Opportunity",
                                "https://${BASE_URL}/services/data/v60.0/sobjects/Opportunity/${currentOpportunityId}")
                                .method(HTTPConstants.DELETE)
                                .children(
                                                responseAssertion()
                                                                .fieldToTest(TargetField.RESPONSE_CODE)
                                                                .equalsToStrings(
                                                                                "204"));
        }

        public DslTransactionController deleteAllOpportunities() {
                return transaction("Opportunities Clean Up",
                                getOpportunities(),
                                forEachController(
                                                "ForEach OpportunityId",
                                                "opportunityId",
                                                "currentOpportunityId",
                                                deleteOpportunity()));
        }

}
