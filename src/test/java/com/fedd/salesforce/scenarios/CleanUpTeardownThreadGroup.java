package com.fedd.salesforce.scenarios;

import static us.abstracta.jmeter.javadsl.JmeterDsl.forEachController;
import static us.abstracta.jmeter.javadsl.JmeterDsl.httpHeaders;
import static us.abstracta.jmeter.javadsl.JmeterDsl.teardownThreadGroup;
import static us.abstracta.jmeter.javadsl.JmeterDsl.transaction;

import com.fedd.salesforce.services.AccountService;
import com.fedd.salesforce.services.LeadService;
import com.fedd.salesforce.services.NoteService;
import com.fedd.salesforce.services.OpportunityService;

import us.abstracta.jmeter.javadsl.core.threadgroups.DslTeardownThreadGroup;

public class CleanUpTeardownThreadGroup {

        private final LeadService leadService = new LeadService();
        private final AccountService accountService = new AccountService();
        private final OpportunityService opportunityService = new OpportunityService();
        private final NoteService noteService = new NoteService();

        public DslTeardownThreadGroup getTeardownThreadGroup() {
                return teardownThreadGroup("Clean up")
                                .children(
                                                httpHeaders()
                                                                .header("Authorization",
                                                                                "Bearer ${__P(ACCESS_TOKEN,)}"),
                                                transaction("Notes Clean Up",
                                                                noteService.getNotes(),
                                                                forEachController(
                                                                                "ForEach NoteId",
                                                                                "noteId",
                                                                                "currentNoteId",
                                                                                noteService
                                                                                                .deleteNote())),


                                                transaction("Opportunities Clean Up",
                                                                opportunityService.getOpportunities(),
                                                                forEachController(
                                                                                "ForEach OpportunityId",
                                                                                "opportunityId",
                                                                                "currentOpportunityId",
                                                                                opportunityService
                                                                                                .deleteOpportunity())),

                                                transaction("Accounts Clean Up",
                                                                accountService.getAccounts(),
                                                                forEachController("ForEach AccountId",
                                                                                "accountId",
                                                                                "currentAccountId",
                                                                                accountService.deleteAccount())),

                                                transaction("Leads Clean Up",
                                                                leadService.getLeads(),
                                                                forEachController("ForEach LeadId",
                                                                                "leadId",
                                                                                "currentLeadId",
                                                                                leadService.deleteLead())));
        }

}
