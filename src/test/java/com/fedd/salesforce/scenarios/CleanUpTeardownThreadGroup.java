package com.fedd.salesforce.scenarios;

import static us.abstracta.jmeter.javadsl.JmeterDsl.httpHeaders;
import static us.abstracta.jmeter.javadsl.JmeterDsl.teardownThreadGroup;

import com.fedd.salesforce.services.AccountService;
import com.fedd.salesforce.services.CaseService;
import com.fedd.salesforce.services.EventService;
import com.fedd.salesforce.services.LeadService;
import com.fedd.salesforce.services.NoteService;
import com.fedd.salesforce.services.OpportunityService;
import com.fedd.salesforce.services.TaskService;

import us.abstracta.jmeter.javadsl.core.threadgroups.DslTeardownThreadGroup;

public class CleanUpTeardownThreadGroup {

        private final LeadService leadService = new LeadService();
        private final AccountService accountService = new AccountService();
        private final OpportunityService opportunityService = new OpportunityService();
        private final NoteService noteService = new NoteService();
        private final TaskService taskService = new TaskService();
        private final CaseService caseService = new CaseService();
        private final EventService eventService = new EventService();

        public DslTeardownThreadGroup getTeardownThreadGroup() {
                return teardownThreadGroup("Clean up")
                                .children(
                                                httpHeaders()
                                                                .header("Authorization",
                                                                                "Bearer ${__P(ACCESS_TOKEN,)}"),
                                                taskService.deleteAllTasks().generateParentSample(),
                                                noteService.deleteAllNotes().generateParentSample(),
                                                opportunityService.deleteAllOpportunities().generateParentSample(),
                                                accountService.deleteAllAccounts().generateParentSample(),
                                                leadService.deleteAllLeads().generateParentSample(),
                                                caseService.deleteAllCases().generateParentSample(),
                                                eventService.deleteAllEvents().generateParentSample());
        }

}
