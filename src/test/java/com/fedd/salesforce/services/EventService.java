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

/**
 * Service class for creating, querying, and deleting Salesforce Event records
 * using the JMeter Java DSL.
 */
public class EventService {
        
        // Salesforce Event fields (StartDateTime and EndDateTime must be in ISO 8601 format: yyyy-MM-ddTHH:mm:ssZ)
        private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

        /**
         * Fetches Event Ids owned by the current user.
         */
        public DslHttpSampler getEvents() {
                return httpSampler("GET Events",
                                "https://${BASE_URL}/services/data/v60.0/query/?q=SELECT+Id+FROM+Event+WHERE+OwnerId='${ownerId}'")
                                .children(
                                        // The extracted variable name is updated to eventId
                                        jsonExtractor("eventId",
                                                        "records[*].Id")
                                                        .matchNumber(-1)
                                                        .defaultValue("eventId_NOT_FOUND"),
                                        jsonAssertion("Success Assertion",
                                                        "$.done")
                                                        .queryLanguage(JsonQueryLanguage.JSON_PATH)
                                                        .equalsToJson("true"));
        }

        /**
         * Creates a new Event record in Salesforce.
         * The body is updated with required Event fields: StartDateTime and EndDateTime.
         */
        public DslHttpSampler createEvent() {
                // The URL is updated to target the Event sObject
                return httpSampler("CREATE New Event",
                                "https://${BASE_URL}/services/data/v60.0/sobjects/Event/")
                                .post("""
                                                {
                                                   "OwnerId": "${ownerId}",
                                                   "WhoId": "${leadId}",
                                                   "Subject": "Internal Meeting",
                                                   "StartDateTime": "${__timeShift(%s,,,,P1D)}",
                                                   "EndDateTime": "${__timeShift(%s,,,,P1H)}"
                                                }
                                                """.formatted(DATE_TIME_FORMAT, DATE_TIME_FORMAT),
                                                ContentType.APPLICATION_JSON)
                                .children(
                                        jsonExtractor("eventId",
                                                        "id")
                                                        .defaultValue("eventId_NOT_FOUND"),
                                        jsonAssertion("Success Assertion",
                                                        "$.success")
                                                        .queryLanguage(JsonQueryLanguage.JSON_PATH)
                                                        .equalsToJson("true"));
        }

        /**
         * Deletes a specific Event record.
         */
        public DslHttpSampler deleteEvent() {
                return httpSampler("DELETE Event",
                                "https://${BASE_URL}/services/data/v60.0/sobjects/Event/${currentEventId}")
                                .method(HTTPConstants.DELETE)
                                .children(
                                        responseAssertion()
                                                        .fieldToTest(TargetField.RESPONSE_CODE)
                                                        .equalsToStrings(
                                                                        "204"));
        }

        /**
         * Retrieves all Events and deletes them using a ForEach Controller.
         */
        public DslTransactionController deleteAllEvents() {
                return transaction("Event Clean Up",
                                getEvents(), // Gets all event Ids
                                forEachController("ForEach EventId",
                                        "eventId",
                                        "currentEventId", 
                                        deleteEvent()));
        }
}