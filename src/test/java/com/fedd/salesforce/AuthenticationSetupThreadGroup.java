package com.fedd.salesforce;

import static us.abstracta.jmeter.javadsl.JmeterDsl.httpSampler;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jsonAssertion;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jsonExtractor;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jsr223PostProcessor;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jsr223PreProcessor;
import static us.abstracta.jmeter.javadsl.JmeterDsl.setupThreadGroup;
import static us.abstracta.jmeter.javadsl.JmeterDsl.transaction;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.jmeter.protocol.http.util.HTTPConstants;

import us.abstracta.jmeter.javadsl.core.postprocessors.DslJsonExtractor.JsonQueryLanguage;
import us.abstracta.jmeter.javadsl.core.threadgroups.DslSetupThreadGroup;
import us.abstracta.jmeter.javadsl.http.DslHttpSampler;

public class AuthenticationSetupThreadGroup {
        public DslSetupThreadGroup getSetupThreadGroup() throws IOException {
                return setupThreadGroup("Authentication",
                                transaction("Authentication")
                                                .generateParentSample()
                                                .children(
                                                                authenticate()));
        }

        private DslHttpSampler authenticate() {
                return httpSampler("Authentication", "https://${BASE_URL}/services/oauth2/token")
                                .method(HTTPConstants.POST)
                                .param("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                                .param("assertion", "${JWT_ASSERTION}")
                                .encoding(StandardCharsets.UTF_8)
                                .children(
                                                jsr223PreProcessor("JWT Generator", loadJwtScript()),

                                                jsonExtractor("access_token", "access_token")
                                                                .defaultValue("access_token_NOT_FOUND"),

                                                jsr223PostProcessor("ACCESS_TOKEN Property",
                                                                "props.put('ACCESS_TOKEN', vars.get('access_token'))"),

                                                jsonAssertion("Access_token Assertion", "$.access_token")
                                                                .queryLanguage(JsonQueryLanguage.JSON_PATH)
                                                                .matches(".*"),
                                                jsonAssertion("Token_type Assertion", "$.token_type")
                                                                .queryLanguage(JsonQueryLanguage.JSON_PATH)
                                                                .equalsToJson("Bearer"));
        }

        private String loadJwtScript() {
                try {
                        var resource = getClass().getResourceAsStream("/scripts/jwt_generator.groovy");

                        if (resource != null) {
                                return new String(resource.readAllBytes(), StandardCharsets.UTF_8);
                        }

                        return java.nio.file.Files.readString(
                                        java.nio.file.Path.of("src/test/resources/scripts/jwt_generator.groovy"),
                                        StandardCharsets.UTF_8);

                } catch (IOException e) {
                        throw new RuntimeException("Failed to load JWT generation script", e);
                }
        }
}
