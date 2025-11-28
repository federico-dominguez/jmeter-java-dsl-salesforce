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

public class CaseService {
        public DslHttpSampler getCases() {
                return httpSampler("GET Cases",
                                "https://${BASE_URL}/services/data/v60.0/query/?q=SELECT+Id+FROM+Case+WHERE+OwnerId='${ownerId}'")
                                .children(
                                                jsonExtractor("caseId",
                                                                "records[*].Id")
                                                                .matchNumber(-1)
                                                                .defaultValue("caseId_NOT_FOUND"),
                                                jsonAssertion("Success Assertion",
                                                                "$.done")
                                                                .queryLanguage(JsonQueryLanguage.JSON_PATH)
                                                                .equalsToJson("true"));
        }

        public DslHttpSampler createCase() {
                return httpSampler("CREATE New Case",
                                "https://${BASE_URL}/services/data/v60.0/sobjects/Case/")
                                .post("""
                                                {
                                                 "ContactId": "003gK00000GNSIoQAP",
                                                 "OwnerId": "${ownerId}",
                                                 "Priority": "High",
                                                 "Priority": "Normal",
                                                 "Reason": "Performance",
                                                 "Status": "New",
                                                 "Subject": "Performance inadequate for second consecutive week",
                                                 "Type" : "Electrical"
                                                }
                                                """,
                                                ContentType.APPLICATION_JSON)
                                .children(
                                                jsonExtractor("caseId",
                                                                "id")
                                                                .defaultValue("caseId_NOT_FOUND"),
                                                jsonAssertion("Success Assertion",
                                                                "$.success")
                                                                .queryLanguage(JsonQueryLanguage.JSON_PATH)
                                                                .equalsToJson("true"));
        }

        public DslHttpSampler deleteCase() {
                return httpSampler("DELETE Case",
                                "https://${BASE_URL}/services/data/v60.0/sobjects/Case/${currentCaseId}")
                                .method(HTTPConstants.DELETE)
                                .children(
                                                responseAssertion()
                                                                .fieldToTest(TargetField.RESPONSE_CODE)
                                                                .equalsToStrings(
                                                                                "204"));
        }

        public DslTransactionController deleteAllCases() {
                return transaction("Case Clean Up",
                                getCases(),
                                forEachController("ForEach CaseId",
                                                "caseId",
                                                "currentCaseId",
                                                deleteCase()));
        }
}
