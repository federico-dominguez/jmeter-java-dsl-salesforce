package com.fedd.salesforce;

import static org.assertj.core.api.Assertions.assertThat;
import static us.abstracta.jmeter.javadsl.JmeterDsl.csvDataSet;
import static us.abstracta.jmeter.javadsl.JmeterDsl.forEachController;
import static us.abstracta.jmeter.javadsl.JmeterDsl.httpCache;
import static us.abstracta.jmeter.javadsl.JmeterDsl.httpCookies;
import static us.abstracta.jmeter.javadsl.JmeterDsl.httpSampler;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jsonAssertion;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jsonExtractor;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jsr223PostProcessor;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jsr223PreProcessor;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jtlWriter;
import static us.abstracta.jmeter.javadsl.JmeterDsl.regexExtractor;
import static us.abstracta.jmeter.javadsl.JmeterDsl.responseAssertion;
import static us.abstracta.jmeter.javadsl.JmeterDsl.setupThreadGroup;
import static us.abstracta.jmeter.javadsl.JmeterDsl.teardownThreadGroup;
import static us.abstracta.jmeter.javadsl.JmeterDsl.testPlan;
import static us.abstracta.jmeter.javadsl.JmeterDsl.threadGroup;
import static us.abstracta.jmeter.javadsl.JmeterDsl.transaction;
import static us.abstracta.jmeter.javadsl.JmeterDsl.vars;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.http.entity.ContentType;
import org.apache.jmeter.protocol.http.util.HTTPConstants;
import org.junit.jupiter.api.Test;

import us.abstracta.jmeter.javadsl.core.DslTestPlan;
import us.abstracta.jmeter.javadsl.core.TestPlanStats;
import us.abstracta.jmeter.javadsl.core.assertions.DslResponseAssertion.TargetField;
import us.abstracta.jmeter.javadsl.core.postprocessors.DslJsonExtractor.JsonQueryLanguage;

public class PerformanceTest {

        @Test
        public void test() throws IOException {
                TestPlanStats stats = getTestPlan().children(
                                jtlWriter("target/jtls")).run();
                assertThat(stats.overall().errorsCount()).isEqualTo(0L);
        }

        @Test
        private DslTestPlan getTestPlan() throws IOException {

                String jwtScript;
                try (InputStream is = getClass().getResourceAsStream("/scripts/jwt_generator.groovy")) {
                        if (is == null) {
                                jwtScript = Files.readString(Path.of("src/test/resources/scripts/jwt_generator.groovy"),
                                                StandardCharsets.UTF_8);
                        } else {
                                jwtScript = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                        }
                }

                return testPlan()
                                .sequentialThreadGroups()
                                .children(
                                                vars().set("BASE_URL",
                                                                "orgfarm-b8d4a27e18-dev-ed.develop.my.salesforce.com"),
                                                csvDataSet(
                                                                "src/main/resources/data/leads_data.csv")
                                                                .ignoreFirstLine()
                                                                .variableNames("p_lastname", "p_company",
                                                                                "p_email_prefix", "p_leadsource",
                                                                                "p_amount"),
                                                httpCache()
                                                                .disable(),
                                                httpCookies()
                                                                .disable(),
                                                setupThreadGroup("Authentication",
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
                                                                                                                                                .equalsToJson("Bearer")))),
                                                threadGroup("Sales Flow Load", 1, 1,
                                                                // uniformRandomTimer(Duration.ofSeconds(4),
                                                                // Duration.ofSeconds(10)),
                                                                transaction("CREATE/UPDATE - Sales Flow",
                                                                                httpSampler("CREATE New Lead",
                                                                                                "https://${BASE_URL}/services/data/v60.0/sobjects/Lead/")
                                                                                                .post("{\r\n"
                                                                                                                + "  \"LastName\": \"${p_lastname}\",\r\n"
                                                                                                                + "  \"Company\": \"${p_company}\",\r\n"
                                                                                                                + "  \"Status\": \"New\",\r\n"
                                                                                                                + "  \"LeadSource\": \"${p_leadsource}\",\r\n"
                                                                                                                + "  \"Email\": \"${p_email_prefix}_${__RandomString(5,123456789)}@perftest.com\"\r\n"
                                                                                                                + "}",
                                                                                                                ContentType.APPLICATION_JSON)
                                                                                                .header("Authorization",
                                                                                                                "Bearer ${__P(ACCESS_TOKEN,)}")
                                                                                                .children(
                                                                                                                jsonExtractor("leadId",
                                                                                                                                "id")
                                                                                                                                .defaultValue("leadId_NOT_FOUND"),
                                                                                                                jsonAssertion("Success Assertion",
                                                                                                                                "$.success")
                                                                                                                                .queryLanguage(JsonQueryLanguage.JSON_PATH)
                                                                                                                                .equalsToJson("true")),
                                                                                httpSampler("UPDATE Lead to Close - Converted",
                                                                                                "https://${BASE_URL}/services/Soap/c/60.0")
                                                                                                .post("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:urn=\"urn:enterprise.soap.sforce.com\">\r\n"
                                                                                                                + "   <soapenv:Header>\r\n"
                                                                                                                + "      <urn:SessionHeader>\r\n"
                                                                                                                + "         <urn:sessionId>${__P(ACCESS_TOKEN,)}</urn:sessionId>\r\n"
                                                                                                                + "      </urn:SessionHeader>\r\n"
                                                                                                                + "   </soapenv:Header>\r\n"
                                                                                                                + "   <soapenv:Body>\r\n"
                                                                                                                + "      <urn:convertLead>\r\n"
                                                                                                                + "         <urn:leadConverts>\r\n"
                                                                                                                + "            <urn:convertedStatus>Closed - Converted</urn:convertedStatus>\r\n"
                                                                                                                + "            <urn:leadId>${leadId}</urn:leadId>\r\n"
                                                                                                                + "            <urn:doNotCreateOpportunity>false</urn:doNotCreateOpportunity>\r\n"
                                                                                                                + "            <urn:opportunityName>Deal for ${leadId}</urn:opportunityName>\r\n"
                                                                                                                + "         </urn:leadConverts>\r\n"
                                                                                                                + "      </urn:convertLead>\r\n"
                                                                                                                + "   </soapenv:Body>\r\n"
                                                                                                                + "</soapenv:Envelope>",
                                                                                                                ContentType.create(
                                                                                                                                "text/xml",
                                                                                                                                "UTF-8"))
                                                                                                .header("Authorization",
                                                                                                                "Bearer ${__P(ACCESS_TOKEN,)}")
                                                                                                .header("SOAPAction",
                                                                                                                "\"\"")
                                                                                                .children(
                                                                                                                regexExtractor("accountId",
                                                                                                                                "<accountId>(.+?)</accountId>"),
                                                                                                                regexExtractor("opportunityId",
                                                                                                                                "<opportunityId>(.+?)</opportunityId>"),
                                                                                                                regexExtractor("contactId",
                                                                                                                                "<contactId>(.+?)</contactId>"),
                                                                                                                jsr223PostProcessor(
                                                                                                                                "RECORD_ID Properties",
                                                                                                                                "def account_Id = vars.get(\"accountId\");\n"
                                                                                                                                                + "def opportunity_Id = vars.get(\"opportunityId\"); \n"
                                                                                                                                                + "def contact_Id = vars.get(\"contactId\"); \n"
                                                                                                                                                + "\n"
                                                                                                                                                + "props.put(\"ACCOUNT_ID\", account_Id);\n"
                                                                                                                                                + "props.put(\"OPPORTUNITY_ID\", opportunity_Id);\n"
                                                                                                                                                + "props.put(\"CONTACT_ID\", contact_Id);"),
                                                                                                                responseAssertion(
                                                                                                                                "Succes Assertion")
                                                                                                                                .containsSubstrings(
                                                                                                                                                "<success>true</success>")),
                                                                                httpSampler("UPDATE Opportunity to Closed Won",
                                                                                                "https://${BASE_URL}/services/data/v60.0/sobjects/Opportunity/${opportunityId}")
                                                                                                .method(HTTPConstants.PATCH)
                                                                                                .contentType(ContentType.APPLICATION_JSON)
                                                                                                .header("Authorization",
                                                                                                                "Bearer ${__P(ACCESS_TOKEN,)}")
                                                                                                .body("{\r\n"
                                                                                                                + "  \"StageName\": \"Closed Won\",\r\n"
                                                                                                                + "  \"Amount\": ${p_amount}\r\n"
                                                                                                                + "}")
                                                                                                .children(
                                                                                                                responseAssertion(
                                                                                                                                "Response Code Assertion")
                                                                                                                                .fieldToTest(TargetField.RESPONSE_CODE)
                                                                                                                                .equalsToStrings(
                                                                                                                                                "204")))),
                                                teardownThreadGroup("Clean up",
                                                                transaction("DELETE - Opportunities Clean Up",
                                                                                httpSampler("GET Opportunities",
                                                                                                "https://${BASE_URL}/services/data/v60.0/query/?q=SELECT+Id+FROM+Opportunity+WHERE+OwnerId='005gK00000AQ0ppQAD'")
                                                                                                .header("Authorization",
                                                                                                                "Bearer ${__P(ACCESS_TOKEN,)}")
                                                                                                .children(
                                                                                                                jsonExtractor("opportunityId",
                                                                                                                                "records[*].Id")
                                                                                                                                .matchNumber(-1)
                                                                                                                                .defaultValue("opportunityId_NOT_FOUND"),
                                                                                                                jsonAssertion("Success Assertion",
                                                                                                                                "$.done")
                                                                                                                                .queryLanguage(JsonQueryLanguage.JSON_PATH)
                                                                                                                                .equalsToJson("true")),
                                                                                forEachController(
                                                                                                "ForEach OpportunityId",
                                                                                                "opportunityId",
                                                                                                "currentOpportunityId",
                                                                                                httpSampler("DELETE Opportunity",
                                                                                                                "https://${BASE_URL}/services/data/v60.0/sobjects/Opportunity/${currentOpportunityId}")
                                                                                                                .method(HTTPConstants.DELETE)
                                                                                                                .header("Authorization",
                                                                                                                                "Bearer ${__P(ACCESS_TOKEN,)}")
                                                                                                                .children(
                                                                                                                                responseAssertion()
                                                                                                                                                .fieldToTest(TargetField.RESPONSE_CODE)
                                                                                                                                                .equalsToStrings(
                                                                                                                                                                "204")))),
                                                                transaction("DELETE - Accounts Clean Up",
                                                                                httpSampler("GET Accounts",
                                                                                                "https://${BASE_URL}/services/data/v60.0/query/?q=SELECT+Id+FROM+Account+WHERE+OwnerId='005gK00000AQ0ppQAD'")
                                                                                                .header("Authorization",
                                                                                                                "Bearer ${__P(ACCESS_TOKEN,)}")
                                                                                                .children(
                                                                                                                jsonExtractor("accountId",
                                                                                                                                "records[*].Id")
                                                                                                                                .matchNumber(-1)
                                                                                                                                .defaultValue("accountId_NOT_FOUND"),
                                                                                                                jsonAssertion("Success Assertion",
                                                                                                                                "$.done")
                                                                                                                                .queryLanguage(JsonQueryLanguage.JSON_PATH)
                                                                                                                                .equalsToJson("true")),
                                                                                forEachController("ForEach AccountId",
                                                                                                "accountId",
                                                                                                "currentAccountId",
                                                                                                httpSampler("DELETE Account",
                                                                                                                "https://${BASE_URL}/services/data/v60.0/sobjects/Account/${currentAccountId}")
                                                                                                                .method(HTTPConstants.DELETE)
                                                                                                                .header("Authorization",
                                                                                                                                "Bearer ${__P(ACCESS_TOKEN,)}")
                                                                                                                .children(
                                                                                                                                responseAssertion()
                                                                                                                                                .fieldToTest(TargetField.RESPONSE_CODE)
                                                                                                                                                .equalsToStrings(
                                                                                                                                                                "204")))),
                                                                transaction("DELETE - Leads Clean Up",
                                                                                httpSampler("GET Leads",
                                                                                                "https://${BASE_URL}/services/data/v60.0/query/?q=SELECT+Id+FROM+Lead+WHERE+OwnerId='005gK00000AQ0ppQAD'")
                                                                                                .header("Authorization",
                                                                                                                "Bearer ${__P(ACCESS_TOKEN,)}")
                                                                                                .children(
                                                                                                                jsonExtractor("leadId",
                                                                                                                                "records[*].Id")
                                                                                                                                .matchNumber(-1)
                                                                                                                                .defaultValue("leadId_NOT_FOUND"),
                                                                                                                jsonAssertion("Success Assertion",
                                                                                                                                "$.done")
                                                                                                                                .queryLanguage(JsonQueryLanguage.JSON_PATH)
                                                                                                                                .equalsToJson("true")),
                                                                                forEachController("ForEach LeadId",
                                                                                                "leadId",
                                                                                                "currentLeadId",
                                                                                                httpSampler("DELETE Lead",
                                                                                                                "https://${BASE_URL}/services/data/v60.0/sobjects/Lead/${currentLeadId}")
                                                                                                                .method(HTTPConstants.DELETE)
                                                                                                                .header("Authorization",
                                                                                                                                "Bearer ${__P(ACCESS_TOKEN,)}")
                                                                                                                .children(
                                                                                                                                responseAssertion()
                                                                                                                                                .fieldToTest(TargetField.RESPONSE_CODE)
                                                                                                                                                .equalsToStrings(
                                                                                                                                                                "204"))))));
        }
}