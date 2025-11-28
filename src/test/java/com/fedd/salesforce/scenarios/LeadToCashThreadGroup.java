package com.fedd.salesforce.scenarios;

import static us.abstracta.jmeter.javadsl.JmeterDsl.httpHeaders;
import static us.abstracta.jmeter.javadsl.JmeterDsl.threadGroup;
import static us.abstracta.jmeter.javadsl.JmeterDsl.transaction;

import com.fedd.salesforce.services.LeadService;
import com.fedd.salesforce.services.OpportunityService;

import us.abstracta.jmeter.javadsl.core.threadgroups.DslDefaultThreadGroup;

public class LeadToCashThreadGroup {

  private final LeadService leadService = new LeadService();
  private final OpportunityService opportunityService = new OpportunityService();

  public DslDefaultThreadGroup getLeadToCashThreadGroup(int users, int iterations) {
    return threadGroup("Lead to Cash", users, iterations,
        // uniformRandomTimer(Duration.ofSeconds(4),
        // Duration.ofSeconds(10)),
        httpHeaders().header("Authorization",
            "Bearer ${__P(ACCESS_TOKEN,)}"),
        transaction("Lead to Cash Process")
            .generateParentSample(),
        leadService.createLead(),
        leadService.convertLead(),
        opportunityService.closeOpportunity());
  }

}