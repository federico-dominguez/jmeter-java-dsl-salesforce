package com.fedd.salesforce.scenarios;

import static us.abstracta.jmeter.javadsl.JmeterDsl.httpHeaders;
import static us.abstracta.jmeter.javadsl.JmeterDsl.ifController;
import static us.abstracta.jmeter.javadsl.JmeterDsl.threadGroup;
import static us.abstracta.jmeter.javadsl.JmeterDsl.uniformRandomTimer;
import static us.abstracta.jmeter.javadsl.JmeterDsl.vars;

import java.time.Duration;

import com.fedd.salesforce.services.CaseService;
import com.fedd.salesforce.services.EventService;
import com.fedd.salesforce.services.LeadService;
import com.fedd.salesforce.services.NoteService;
import com.fedd.salesforce.services.OpportunityService;
import com.fedd.salesforce.services.TaskService;

import us.abstracta.jmeter.javadsl.core.threadgroups.DslDefaultThreadGroup;

/**
 * Main thread group implementing the Lead-to-Cash business workflow.
 * <p>
 * Simulates real user behavior with configurable probability for optional steps
 * (notes, tasks, events, cases) and conditional lead conversion / opportunity closing.
 * </p>
 */
public class LeadToCashThreadGroup {

        private final LeadService leadService = new LeadService();
        private final OpportunityService opportunityService = new OpportunityService();
        private final NoteService noteService = new NoteService();
        private final TaskService taskService = new TaskService();
        private final EventService eventService = new EventService();
        private final CaseService caseService = new CaseService();

        // Parameter defaults
        private static final String NOTE_CHANCE = "${__P(NOTE_CHANCE,65)}";
        private static final String TASK_CHANCE = "${__P(TASK_CHANCE,75)}";
        private static final String EVENT_CHANCE = "${__P(EVENT_CHANCE,20)}";
        private static final String CASE_CHANCE = "${__P(CASE_CHANCE,20)}";
        private static final String CONVERSION_RATE = "${__P(CONVERSION_RATE,35)}";
        private static final String CLOSING_RATE = "${__P(CLOSING_RATE,50)}";

        public DslDefaultThreadGroup getLeadToCashThreadGroup(int users, int iterations) {

                return threadGroup("Lead to Cash", users, iterations,

                                httpHeaders()
                                                .header("Authorization", "Bearer ${__P(ACCESS_TOKEN,)}"),

                                // Load percentages into JMeter variables correctly
                                vars()
                                                .set("NOTE_CHANCE", NOTE_CHANCE)
                                                .set("TASK_CHANCE", TASK_CHANCE)
                                                .set("EVENT_CHANCE", EVENT_CHANCE)
                                                .set("CASE_CHANCE", CASE_CHANCE)
                                                .set("LEAD_CONVERSION_RATE", CONVERSION_RATE)
                                                .set("CLOSING_RATE", CLOSING_RATE),

                                // Random think time between 4-6 seconds
                                uniformRandomTimer(Duration.ofSeconds(4), Duration.ofSeconds(6)),

                                leadService.createLead(),

                                // ---- Conditional Note ----
                                ifController("${__groovy(new Random().nextInt(100) + 1 <= ${NOTE_CHANCE})}",
                                                noteService.createNote()),

                                // ---- Conditional Task ----
                                ifController("${__groovy(new Random().nextInt(100) + 1 <= ${TASK_CHANCE})}",
                                                taskService.createTask()),

                                // ---- Conditional Event ----
                                ifController("${__groovy(new Random().nextInt(100) + 1 <= ${EVENT_CHANCE})}",
                                                eventService.createEvent()),

                                // ---- Conditional Case ----
                                ifController("${__groovy(new Random().nextInt(100) + 1 <= ${CASE_CHANCE})}",
                                                caseService.createCase()),

                                // ---- Lead Conversion ----
                                ifController("${__groovy(new Random().nextInt(100) + 1 <= ${LEAD_CONVERSION_RATE})}",
                                                leadService.convertLead(),

                                                // ---- Conditional Opportunity Closing ----
                                                ifController("${__groovy(new Random().nextInt(100) + 1 <= ${CLOSING_RATE})}",
                                                                opportunityService.closeOpportunity())));
        }
}
