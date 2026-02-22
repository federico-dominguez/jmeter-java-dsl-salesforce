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
 * Service class for creating, querying, and deleting Salesforce Note records
 * using the JMeter Java DSL.
 *
 * <p>Inherits standard GET, DELETE, and bulk-delete operations from
 * {@link AbstractSalesforceService}.</p>
 */
public class NoteService extends AbstractSalesforceService {

        @Override protected String sObjectName()       { return "Note"; }
        @Override protected String idVariable()         { return "noteId"; }
        @Override protected String currentIdVariable()  { return "currentNoteId"; }
        @Override protected String displayName()        { return "Note"; }

        /** @deprecated Use {@link #getByOwner()} instead. */
        public DslHttpSampler getNotes() { return getByOwner(); }

        /** @deprecated Use {@link #getAll()} instead. */
        public DslHttpSampler getAllNotes() { return getAll(); }

        /** @deprecated Use {@link #deleteRecord()} instead. */
        public DslHttpSampler deleteNote() { return deleteRecord(); }

        /**
         * Creates a new Note record linked to the current Lead.
         *
         * @return an HTTP sampler that POSTs a Note and validates the response
         */
        public DslHttpSampler createNote() {
                return httpSampler("CREATE New Note", TestConfig.restUrl("Note"))
                                .post("""
                                                {
                                                 "Body": "Meeting notes for ${p_lastname}",
                                                 "IsPrivate": "false",
                                                 "OwnerId": "${ownerId}",
                                                 "ParentId": "${leadId}",
                                                 "Title": "Meeting Notes"
                                                }
                                                """,
                                                ContentType.APPLICATION_JSON)
                                .children(
                                                jsonExtractor("noteId", "id")
                                                                .defaultValue("noteId_NOT_FOUND"),
                                                jsonAssertion("Success Assertion", "$.success")
                                                                .queryLanguage(JsonQueryLanguage.JSON_PATH)
                                                                .equalsToJson("true"));
        }

        /** @deprecated Use {@link #deleteAll()} instead. */
        public DslTransactionController deleteAllNotes() { return deleteAll(); }
}
