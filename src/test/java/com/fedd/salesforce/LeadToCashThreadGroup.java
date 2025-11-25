package com.fedd.salesforce;

import static us.abstracta.jmeter.javadsl.JmeterDsl.httpSampler;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jsonAssertion;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jsonExtractor;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jsr223PostProcessor;
import static us.abstracta.jmeter.javadsl.JmeterDsl.regexExtractor;
import static us.abstracta.jmeter.javadsl.JmeterDsl.responseAssertion;
import static us.abstracta.jmeter.javadsl.JmeterDsl.threadGroup;
import static us.abstracta.jmeter.javadsl.JmeterDsl.transaction;

import org.apache.http.entity.ContentType;
import org.apache.jmeter.protocol.http.util.HTTPConstants;

import us.abstracta.jmeter.javadsl.core.assertions.DslResponseAssertion.TargetField;
import us.abstracta.jmeter.javadsl.core.postprocessors.DslJsonExtractor.JsonQueryLanguage;
import us.abstracta.jmeter.javadsl.core.threadgroups.DslDefaultThreadGroup;
import us.abstracta.jmeter.javadsl.http.DslHttpSampler;

public class LeadToCashThreadGroup {
    public DslDefaultThreadGroup getLeadToCashThreadGroup() {
        return threadGroup("Lead to Cash", 1, 1,
                // uniformRandomTimer(Duration.ofSeconds(4),
                // Duration.ofSeconds(10)),
                transaction("Lead to Cash Process")
                        .generateParentSample(),
                        createLead(),
                        convertLead(),
                        closeOpportunity());
    }

    private DslHttpSampler createLead() {
        return httpSampler("CREATE New Lead",
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
                                .equalsToJson("true"));
    }

    private DslHttpSampler convertLead() {
        return httpSampler("UPDATE Lead to Close - Converted",
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
                                        "<success>true</success>"));
    }

    private DslHttpSampler closeOpportunity() {
        return httpSampler("UPDATE Opportunity to Closed Won",
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
                                        "204"));
    }
}