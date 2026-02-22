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
 * Service class for creating, querying, and deleting Salesforce Event records
 * using the JMeter Java DSL.
 *
 * <p>Inherits standard GET, DELETE, and bulk-delete operations from
 * {@link AbstractSalesforceService}.</p>
 */
public class EventService extends AbstractSalesforceService {

        // Salesforce Event fields (StartDateTime and EndDateTime must be in ISO 8601 format)
        private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

        @Override protected String sObjectName()       { return "Event"; }
        @Override protected String idVariable()         { return "eventId"; }
        @Override protected String currentIdVariable()  { return "currentEventId"; }
        @Override protected String displayName()        { return "Event"; }

        /** @deprecated Use {@link #getByOwner()} instead. */
        public DslHttpSampler getEvents() { return getByOwner(); }

        /** @deprecated Use {@link #getAll()} instead. */
        public DslHttpSampler getAllEvents() { return getAll(); }

        /** @deprecated Use {@link #deleteRecord()} instead. */
        public DslHttpSampler deleteEvent() { return deleteRecord(); }

        /**
         * Creates a new Event record in Salesforce.
         * Uses ISO 8601 date format for StartDateTime and EndDateTime fields.
         *
         * @return an HTTP sampler that POSTs an Event and validates the response
         */
        public DslHttpSampler createEvent() {
                return httpSampler("CREATE New Event", TestConfig.restUrl("Event"))
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
                                                jsonExtractor("eventId", "id")
                                                                .defaultValue("eventId_NOT_FOUND"),
                                                jsonAssertion("Success Assertion", "$.success")
                                                                .queryLanguage(JsonQueryLanguage.JSON_PATH)
                                                                .equalsToJson("true"));
        }

        /** @deprecated Use {@link #deleteAll()} instead. */
        public DslTransactionController deleteAllEvents() { return deleteAll(); }
}