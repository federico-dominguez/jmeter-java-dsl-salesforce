package com.fedd.salesforce.services;

import static us.abstracta.jmeter.javadsl.JmeterDsl.httpSampler;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jsonAssertion;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jsonExtractor;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jsr223PostProcessor;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jsr223PreProcessor;

import com.fedd.salesforce.config.TestConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.jmeter.protocol.http.util.HTTPConstants;

import us.abstracta.jmeter.javadsl.core.postprocessors.DslJsonExtractor.JsonQueryLanguage;
import us.abstracta.jmeter.javadsl.http.DslHttpSampler;

/**
 * Service class for Salesforce OAuth 2.0 JWT Bearer Flow authentication
 * using the JMeter Java DSL.
 *
 * <p>Generates a JWT assertion via a Groovy script, exchanges it for an
 * access token, and stores the token as a JMeter property for use by
 * subsequent thread groups.</p>
 */
public class AuthService {

    /**
     * Authenticates against Salesforce using the JWT Bearer Flow.
     *
     * @return an HTTP sampler that POSTs to the OAuth2 token endpoint
     */
    public DslHttpSampler authenticate() {
        return httpSampler("Authentication", TestConfig.authUrl())
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
