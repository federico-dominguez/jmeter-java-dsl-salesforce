package com.fedd.salesforce;

import static us.abstracta.jmeter.javadsl.JmeterDsl.httpSampler;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jsonAssertion;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jsonExtractor;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jsr223PostProcessor;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jsr223PreProcessor;
import static us.abstracta.jmeter.javadsl.JmeterDsl.setupThreadGroup;
import static us.abstracta.jmeter.javadsl.JmeterDsl.transaction;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.jmeter.protocol.http.util.HTTPConstants;

import us.abstracta.jmeter.javadsl.core.postprocessors.DslJsonExtractor.JsonQueryLanguage;
import us.abstracta.jmeter.javadsl.core.threadgroups.DslSetupThreadGroup;

public class AuthenticationSetupThreadGroup {
    public DslSetupThreadGroup getSetupThreadGroup() throws IOException {

        String jwtScript;
        try (InputStream is = getClass().getResourceAsStream("/scripts/jwt_generator.groovy")) {
            if (is == null) {
                jwtScript = Files.readString(Path.of("src/test/resources/scripts/jwt_generator.groovy"),
                        StandardCharsets.UTF_8);
            } else {
                jwtScript = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        return setupThreadGroup("Authentication",
                transaction("LOGIN - Authentication")
                        .generateParentSample()
                        .children(
                                httpSampler("Authentication",
                                        "https://${BASE_URL}/services/oauth2/token")
                                        .method(HTTPConstants.POST)
                                        .param("grant_type",
                                                "urn:ietf:params:oauth:grant-type:jwt-bearer")
                                        .param("assertion",
                                                "${JWT_ASSERTION}")
                                        .encoding(StandardCharsets.UTF_8)
                                        .children(
                                                jsr223PreProcessor(
                                                        "JWT Generator",
                                                        jwtScript),
                                                jsonExtractor("access_token",
                                                        "access_token")
                                                        .defaultValue("access_token_NOT_FOUND"),
                                                jsr223PostProcessor(
                                                        "ACCESS_TOKEN Property",
                                                        "def accessToken = vars.get(\"access_token\"); \n"
                                                                + "\n"
                                                                + "props.put(\"ACCESS_TOKEN\", accessToken);"),
                                                jsonAssertion("Access_token Assertion",
                                                        "$.access_token")
                                                        .queryLanguage(JsonQueryLanguage.JSON_PATH)
                                                        .matches(".*"),
                                                jsonAssertion("Token_type Assertion",
                                                        "$.token_type")
                                                        .queryLanguage(JsonQueryLanguage.JSON_PATH)
                                                        .equalsToJson("Bearer"))));
    }
}
