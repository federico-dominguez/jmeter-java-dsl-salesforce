package com.fedd.salesforce.scenarios;

import static us.abstracta.jmeter.javadsl.JmeterDsl.httpHeaders;
import static us.abstracta.jmeter.javadsl.JmeterDsl.threadGroup;
import static us.abstracta.jmeter.javadsl.JmeterDsl.transaction;

import com.fedd.salesforce.services.CaseService;
import com.fedd.salesforce.services.LeadService;

import us.abstracta.jmeter.javadsl.core.threadgroups.DslDefaultThreadGroup;

public class TestServiceThreadGroup {

    private final LeadService leadService = new LeadService();
    private final CaseService caseService = new CaseService();

    public DslDefaultThreadGroup getServiceThreadGroup(int users, int iterations) {
        return threadGroup("Lead to Cash", users, iterations,
                httpHeaders().header("Authorization",
                        "Bearer ${__P(ACCESS_TOKEN,)}"),
                transaction("Test Services Process")
                        .generateParentSample(),
                leadService.createLead(),
                caseService.createCase()
                );
    }

}
