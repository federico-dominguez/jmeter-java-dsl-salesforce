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

public class NoteService {
        public DslHttpSampler getNotes() {
                return httpSampler("GET Notes",
                                "https://${BASE_URL}/services/data/v60.0/query/?q=SELECT+Id+FROM+Note+WHERE+OwnerId='${ownerId}'")
                                .children(
                                                jsonExtractor("noteId",
                                                                "records[*].Id")
                                                                .matchNumber(-1)
                                                                .defaultValue("noteId_NOT_FOUND"),
                                                jsonAssertion("Success Assertion",
                                                                "$.done")
                                                                .queryLanguage(JsonQueryLanguage.JSON_PATH)
                                                                .equalsToJson("true"));
        }

        public DslHttpSampler getAllNotes() {
                return httpSampler("GET All Notes",
                                "https://${BASE_URL}/services/data/v60.0/query/?q=SELECT+Id+FROM+Note")
                                .children(
                                                jsonExtractor("noteId",
                                                                "records[*].Id")
                                                                .matchNumber(-1)
                                                                .defaultValue("noteId_NOT_FOUND"),
                                                jsonAssertion("Success Assertion",
                                                                "$.done")
                                                                .queryLanguage(JsonQueryLanguage.JSON_PATH)
                                                                .equalsToJson("true"));
        }

        public DslHttpSampler createNote() {
                return httpSampler("CREATE New Note",
                                "https://${BASE_URL}/services/data/v60.0/sobjects/Note/")
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
                                                jsonExtractor("noteId",
                                                                "id")
                                                                .defaultValue("noteId_NOT_FOUND"),
                                                jsonAssertion("Success Assertion",
                                                                "$.success")
                                                                .queryLanguage(JsonQueryLanguage.JSON_PATH)
                                                                .equalsToJson("true"));
        }

        public DslHttpSampler deleteNote() {
                return httpSampler("DELETE Note",
                                "https://${BASE_URL}/services/data/v60.0/sobjects/Note/${currentNoteId}")
                                .method(HTTPConstants.DELETE)
                                .children(
                                                responseAssertion()
                                                                .fieldToTest(TargetField.RESPONSE_CODE)
                                                                .equalsToStrings(
                                                                                "204"));
        }

        public DslTransactionController deleteAllNotes() {
                return transaction("Notes Clean Up",
                                getNotes(),
                                forEachController(
                                                "ForEach NoteId",
                                                "noteId",
                                                "currentNoteId",
                                                deleteNote()));
        }
}
