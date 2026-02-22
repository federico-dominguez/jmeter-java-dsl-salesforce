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

    /**
     * Builds the thread group with think time enabled (default for real tests).
     */
    public DslDefaultThreadGroup getLeadToCashThreadGroup(int users, int iterations) {
        return getLeadToCashThreadGroup(users, iterations, true);
    }

    /**
     * Builds the thread group with configurable think time.
     *
     * @param users         number of concurrent users
     * @param iterations    iterations per user
     * @param withThinkTime if {@code false}, skips the 4-6s random timer (useful for debug runs)
     */
    public DslDefaultThreadGroup getLeadToCashThreadGroup(int users, int iterations, boolean withThinkTime) {
        return getLeadToCashThreadGroup(users, iterations, withThinkTime, false);
    }

    /**
     * Builds the thread group with full control over think time and step execution.
     *
     * @param users         number of concurrent users
     * @param iterations    iterations per user
     * @param withThinkTime if {@code false}, skips the 4-6s random timer
     * @param forceAllSteps if {@code true}, sets all probabilities to 100% so every
     *                      branch executes (useful for debug/validation runs)
     */
    public DslDefaultThreadGroup getLeadToCashThreadGroup(int users, int iterations,
            boolean withThinkTime, boolean forceAllSteps) {

        String noteChance     = forceAllSteps ? "100" : NOTE_CHANCE;
        String taskChance     = forceAllSteps ? "100" : TASK_CHANCE;
        String eventChance    = forceAllSteps ? "100" : EVENT_CHANCE;
        String caseChance     = forceAllSteps ? "100" : CASE_CHANCE;
        String conversionRate = forceAllSteps ? "100" : CONVERSION_RATE;
        String closingRate    = forceAllSteps ? "100" : CLOSING_RATE;

        DslDefaultThreadGroup tg = threadGroup("Lead to Cash", users, iterations,

                httpHeaders()
                        .header("Authorization", "Bearer ${__P(ACCESS_TOKEN,)}"),

                // Load percentages into JMeter variables
                vars()
                        .set("NOTE_CHANCE", noteChance)
                        .set("TASK_CHANCE", taskChance)
                        .set("EVENT_CHANCE", eventChance)
                        .set("CASE_CHANCE", caseChance)
                        .set("LEAD_CONVERSION_RATE", conversionRate)
                        .set("CLOSING_RATE", closingRate));

        if (withThinkTime) {
            // Random think time between 4-6 seconds
            tg.children(
                    uniformRandomTimer(Duration.ofSeconds(4), Duration.ofSeconds(6)));
        }

        tg.children(
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

        return tg;
    }
}
