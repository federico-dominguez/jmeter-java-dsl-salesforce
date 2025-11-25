package com.fedd.salesforce;

import static us.abstracta.jmeter.javadsl.JmeterDsl.forEachController;
import static us.abstracta.jmeter.javadsl.JmeterDsl.httpSampler;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jsonAssertion;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jsonExtractor;
import static us.abstracta.jmeter.javadsl.JmeterDsl.responseAssertion;
import static us.abstracta.jmeter.javadsl.JmeterDsl.teardownThreadGroup;
import static us.abstracta.jmeter.javadsl.JmeterDsl.transaction;

import org.apache.jmeter.protocol.http.util.HTTPConstants;

import us.abstracta.jmeter.javadsl.core.assertions.DslResponseAssertion.TargetField;
import us.abstracta.jmeter.javadsl.core.postprocessors.DslJsonExtractor.JsonQueryLanguage;
import us.abstracta.jmeter.javadsl.core.threadgroups.DslTeardownThreadGroup;
import us.abstracta.jmeter.javadsl.http.DslHttpSampler;

public class CleanUpTeardownThreadGroup {

    public DslTeardownThreadGroup getTeardownThreadGroup() {
        // La función estática teardownThreadGroup() se llama aquí
        return teardownThreadGroup("Clean up")
                .children(
                        transaction("Opportunities Clean Up",
                                getOpportunities(),
                                forEachController(
                                        "ForEach OpportunityId",
                                        "opportunityId",
                                        "currentOpportunityId",
                                        deleteOpportunity())),

                        transaction("Accounts Clean Up",
                                getAccounts(),
                                forEachController("ForEach AccountId",
                                        "accountId",
                                        "currentAccountId",
                                        deleteAccount())),

                        transaction("Leads Clean Up",
                                getLeads(),
                                forEachController("ForEach LeadId",
                                        "leadId",
                                        "currentLeadId",
                                        deleteLead()))

                );
    }

    private DslHttpSampler getOpportunities() {
        return httpSampler("GET Opportunities",
                "https://${BASE_URL}/services/data/v60.0/query/?q=SELECT+Id+FROM+Opportunity+WHERE+OwnerId='005gK00000AQ0ppQAD'")
                .header("Authorization",
                        "Bearer ${__P(ACCESS_TOKEN,)}")
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

    private DslHttpSampler deleteOpportunity() {
        return httpSampler("DELETE Opportunity",
                "https://${BASE_URL}/services/data/v60.0/sobjects/Opportunity/${currentOpportunityId}")
                .method(HTTPConstants.DELETE)
                .header("Authorization",
                        "Bearer ${__P(ACCESS_TOKEN,)}")
                .children(
                        responseAssertion()
                                .fieldToTest(TargetField.RESPONSE_CODE)
                                .equalsToStrings(
                                        "204"));
    }

    private DslHttpSampler getAccounts() {
        return httpSampler("GET Accounts",
                "https://${BASE_URL}/services/data/v60.0/query/?q=SELECT+Id+FROM+Account+WHERE+OwnerId='005gK00000AQ0ppQAD'")
                .header("Authorization",
                        "Bearer ${__P(ACCESS_TOKEN,)}")
                .children(
                        jsonExtractor("accountId",
                                "records[*].Id")
                                .matchNumber(-1)
                                .defaultValue("accountId_NOT_FOUND"),
                        jsonAssertion("Success Assertion",
                                "$.done")
                                .queryLanguage(JsonQueryLanguage.JSON_PATH)
                                .equalsToJson("true"));
    }

    private DslHttpSampler deleteAccount() {
        return httpSampler("DELETE Account",
                "https://${BASE_URL}/services/data/v60.0/sobjects/Account/${currentAccountId}")
                .method(HTTPConstants.DELETE)
                .header("Authorization",
                        "Bearer ${__P(ACCESS_TOKEN,)}")
                .children(
                        responseAssertion()
                                .fieldToTest(TargetField.RESPONSE_CODE)
                                .equalsToStrings(
                                        "204"));
    }

    private DslHttpSampler getLeads() {
        return httpSampler("GET Leads",
                "https://${BASE_URL}/services/data/v60.0/query/?q=SELECT+Id+FROM+Lead+WHERE+OwnerId='005gK00000AQ0ppQAD'")
                .header("Authorization",
                        "Bearer ${__P(ACCESS_TOKEN,)}")
                .children(
                        jsonExtractor("leadId",
                                "records[*].Id")
                                .matchNumber(-1)
                                .defaultValue("leadId_NOT_FOUND"),
                        jsonAssertion("Success Assertion",
                                "$.done")
                                .queryLanguage(JsonQueryLanguage.JSON_PATH)
                                .equalsToJson("true"));
    }

    private DslHttpSampler deleteLead() {
        return httpSampler("DELETE Lead",
                "https://${BASE_URL}/services/data/v60.0/sobjects/Lead/${currentLeadId}")
                .method(HTTPConstants.DELETE)
                .header("Authorization",
                        "Bearer ${__P(ACCESS_TOKEN,)}")
                .children(
                        responseAssertion()
                                .fieldToTest(TargetField.RESPONSE_CODE)
                                .equalsToStrings(
                                        "204"));
    }

}
