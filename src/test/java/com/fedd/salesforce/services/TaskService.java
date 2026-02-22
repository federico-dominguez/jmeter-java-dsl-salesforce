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
 * Service class for creating, querying, and deleting Salesforce Task records
 * using the JMeter Java DSL.
 *
 * <p>Inherits standard GET, DELETE, and bulk-delete operations from
 * {@link AbstractSalesforceService}.</p>
 */
public class TaskService extends AbstractSalesforceService {

        @Override protected String sObjectName()       { return "Task"; }
        @Override protected String idVariable()         { return "taskId"; }
        @Override protected String currentIdVariable()  { return "currentTaskId"; }
        @Override protected String displayName()        { return "Task"; }

        /** @deprecated Use {@link #getByOwner()} instead. */
        public DslHttpSampler getTasks() { return getByOwner(); }

        /** @deprecated Use {@link #getAll()} instead. */
        public DslHttpSampler getAllTasks() { return getAll(); }

        /** @deprecated Use {@link #deleteRecord()} instead. */
        public DslHttpSampler deleteTask() { return deleteRecord(); }

        /**
         * Creates a new Task record linked to the current Lead.
         *
         * @return an HTTP sampler that POSTs a Task and validates the response
         */
        public DslHttpSampler createTask() {
                return httpSampler("CREATE New Task", TestConfig.restUrl("Task"))
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
                                                jsonExtractor("taskId", "id")
                                                                .defaultValue("taskId_NOT_FOUND"),
                                                jsonAssertion("Success Assertion", "$.success")
                                                                .queryLanguage(JsonQueryLanguage.JSON_PATH)
                                                                .equalsToJson("true"));
        }

        /** @deprecated Use {@link #deleteAll()} instead. */
        public DslTransactionController deleteAllTasks() { return deleteAll(); }
}
