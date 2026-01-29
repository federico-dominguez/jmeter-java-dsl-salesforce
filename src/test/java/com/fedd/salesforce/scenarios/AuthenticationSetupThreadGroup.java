package com.fedd.salesforce.scenarios;

import static us.abstracta.jmeter.javadsl.JmeterDsl.setupThreadGroup;
import static us.abstracta.jmeter.javadsl.JmeterDsl.transaction;

import java.io.IOException;

import com.fedd.salesforce.services.AuthService;

import us.abstracta.jmeter.javadsl.core.threadgroups.DslSetupThreadGroup;

public class AuthenticationSetupThreadGroup {

        private final AuthService authService = new AuthService();

        public DslSetupThreadGroup getSetupThreadGroup() throws IOException {
                return setupThreadGroup("Authentication",
                                authService.authenticate());
        }

}
