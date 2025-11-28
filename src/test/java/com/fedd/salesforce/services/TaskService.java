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

public class TaskService {
        public DslHttpSampler getTasks() {
                return httpSampler("GET Tasks",
                                "https://${BASE_URL}/services/data/v60.0/query/?q=SELECT+Id+FROM+Task+WHERE+OwnerId='${ownerId}'")
                                .children(
                                                jsonExtractor("taskId",
                                                                "records[*].Id")
                                                                .matchNumber(-1)
                                                                .defaultValue("taskId_NOT_FOUND"),
                                                jsonAssertion("Success Assertion",
                                                                "$.done")
                                                                .queryLanguage(JsonQueryLanguage.JSON_PATH)
                                                                .equalsToJson("true"));
        }

        public DslHttpSampler createTask() {
                return httpSampler("CREATE New Task",
                                "https://${BASE_URL}/services/data/v60.0/sobjects/Task/")
                                .post("""
                                                {
                                                 "ActivityDate": "${__timeShift(yyyy-MM-dd,,,,P1D)}",
                                                 "OwnerId": "${ownerId}",
                                                 "WhoId": "${leadId}",
                                                 "Priority": "Normal",
                                                 "Status": "Not Started",
                                                 "Subject": "Call",
                                                 "TaskSubtype": "Task"
                                                }
                                                """,
                                                ContentType.APPLICATION_JSON)
                                .children(
                                                jsonExtractor("taskId",
                                                                "id")
                                                                .defaultValue("taskId_NOT_FOUND"),
                                                jsonAssertion("Success Assertion",
                                                                "$.success")
                                                                .queryLanguage(JsonQueryLanguage.JSON_PATH)
                                                                .equalsToJson("true"));
        }

        public DslHttpSampler deleteTask() {
                return httpSampler("DELETE Task",
                                "https://${BASE_URL}/services/data/v60.0/sobjects/Task/${currentTaskId}")
                                .method(HTTPConstants.DELETE)
                                .children(
                                                responseAssertion()
                                                                .fieldToTest(TargetField.RESPONSE_CODE)
                                                                .equalsToStrings(
                                                                                "204"));
        }

        public DslTransactionController deleteAllTasks() {
                return transaction("Task Clean Up",
                                getTasks(),
                                forEachController("ForEach TaskId",
                                                "taskId",
                                                "currentTaskId",
                                                deleteTask()));
        }
}
