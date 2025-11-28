package com.fedd.salesforce.services;

import static us.abstracta.jmeter.javadsl.JmeterDsl.forEachController;
import static us.abstracta.jmeter.javadsl.JmeterDsl.httpSampler;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jsonAssertion;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jsonExtractor;
import static us.abstracta.jmeter.javadsl.JmeterDsl.responseAssertion;
import static us.abstracta.jmeter.javadsl.JmeterDsl.transaction;

import org.apache.jmeter.protocol.http.util.HTTPConstants;

import us.abstracta.jmeter.javadsl.core.assertions.DslResponseAssertion.TargetField;
import us.abstracta.jmeter.javadsl.core.controllers.DslTransactionController;
import us.abstracta.jmeter.javadsl.core.postprocessors.DslJsonExtractor.JsonQueryLanguage;
import us.abstracta.jmeter.javadsl.http.DslHttpSampler;

public class AccountService {
        public DslHttpSampler getAccounts() {
                return httpSampler("GET Accounts",
                                "https://${BASE_URL}/services/data/v60.0/query/?q=SELECT+Id+FROM+Account+WHERE+OwnerId='${ownerId}'")
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

        public DslHttpSampler deleteAccount() {
                return httpSampler("DELETE Account",
                                "https://${BASE_URL}/services/data/v60.0/sobjects/Account/${currentAccountId}")
                                .method(HTTPConstants.DELETE)
                                .children(
                                                responseAssertion()
                                                                .fieldToTest(TargetField.RESPONSE_CODE)
                                                                .equalsToStrings(
                                                                                "204"));
        }

        public DslTransactionController deleteAllAccounts() {
                return transaction("Accounts Clean Up",
                                getAccounts(),
                                forEachController("ForEach AccountId",
                                                "accountId",
                                                "currentAccountId",
                                                deleteAccount()));
        }
}
