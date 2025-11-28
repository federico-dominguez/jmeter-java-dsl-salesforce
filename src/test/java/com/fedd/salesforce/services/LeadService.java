package com.fedd.salesforce.services;

import static us.abstracta.jmeter.javadsl.JmeterDsl.httpSampler;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jsonAssertion;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jsonExtractor;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jsr223PostProcessor;
import static us.abstracta.jmeter.javadsl.JmeterDsl.regexExtractor;
import static us.abstracta.jmeter.javadsl.JmeterDsl.responseAssertion;

import org.apache.http.entity.ContentType;
import org.apache.jmeter.protocol.http.util.HTTPConstants;

import us.abstracta.jmeter.javadsl.core.assertions.DslResponseAssertion.TargetField;
import us.abstracta.jmeter.javadsl.core.postprocessors.DslJsonExtractor.JsonQueryLanguage;
import us.abstracta.jmeter.javadsl.http.DslHttpSampler;

public class LeadService {

    public DslHttpSampler getLeads() {
        return httpSampler("GET Leads",
                "https://${BASE_URL}/services/data/v60.0/query/?q=SELECT+Id+FROM+Lead+WHERE+OwnerId='${ownerId}'")
                .children(
                        jsonExtractor("leadId",
                                "records[*].Id")
                                .matchNumber(-1)
                                .defaultValue("leadId_NOT_FOUND"),
                        jsonAssertion("Success Assertion",
                                "$.done")
                                .queryLanguage(JsonQueryLanguage.JSON_PATH)
                                .equalsToJson("true"));
    }



    public DslHttpSampler createLead() {
                return httpSampler("CREATE New Lead",
                                "https://${BASE_URL}/services/data/v60.0/sobjects/Lead/")
                                .post("""
                                                {
                                                 "LastName": "${p_lastname}",
                                                 "Company": "${p_company}",
                                                 "Status": "New",
                                                 "LeadSource": "${p_leadsource}",
                                                 "Email": "${p_email_prefix}_${__RandomString(5,123456789)}@perftest.com"
                                                }
                                                """,
                                                ContentType.APPLICATION_JSON)
                                .children(
                                                jsonExtractor("leadId",
                                                                "id")
                                                                .defaultValue("leadId_NOT_FOUND"),
                                                jsonAssertion("Success Assertion",
                                                                "$.success")
                                                                .queryLanguage(JsonQueryLanguage.JSON_PATH)
                                                                .equalsToJson("true"));
        }

        public DslHttpSampler convertLead() {
                return httpSampler("UPDATE Lead to Close - Converted",
                                "https://${BASE_URL}/services/Soap/c/60.0")
                                .post("""
                                                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:urn="urn:enterprise.soap.sforce.com">
                                                  <soapenv:Header>
                                                    <urn:SessionHeader>
                                                      <urn:sessionId>${__P(ACCESS_TOKEN,)}</urn:sessionId>
                                                    </urn:SessionHeader>
                                                  </soapenv:Header>
                                                  <soapenv:Body>
                                                    <urn:convertLead>
                                                      <urn:leadConverts>
                                                        <urn:convertedStatus>Closed - Converted</urn:convertedStatus>
                                                        <urn:leadId>${leadId}</urn:leadId>
                                                        <urn:doNotCreateOpportunity>false</urn:doNotCreateOpportunity>
                                                        <urn:opportunityName>Deal for ${leadId}</urn:opportunityName>
                                                      </urn:leadConverts>
                                                    </urn:convertLead>
                                                  </soapenv:Body>
                                                </soapenv:Envelope>
                                                """,
                                                ContentType.create("text/xml", "UTF-8"))
                                .header("SOAPAction",
                                                "\"\"")
                                .children(
                                                regexExtractor("accountId",
                                                                "<accountId>(.+?)</accountId>"),
                                                regexExtractor("opportunityId",
                                                                "<opportunityId>(.+?)</opportunityId>"),
                                                regexExtractor("contactId",
                                                                "<contactId>(.+?)</contactId>"),
                                                jsr223PostProcessor("RECORD_ID Properties",
                                                                "props.put('ACCOUNT_ID', vars.get('accountId')); "
                                                                                + "props.put('OPPORTUNITY_ID', vars.get('opportunityId')); "
                                                                                + "props.put('CONTACT_ID', vars.get('contactId'));"),
                                                responseAssertion(
                                                                "Succes Assertion")
                                                                .containsSubstrings(
                                                                                "<success>true</success>"));
        }

        public DslHttpSampler deleteLead() {
        return httpSampler("DELETE Lead",
                "https://${BASE_URL}/services/data/v60.0/sobjects/Lead/${currentLeadId}")
                .method(HTTPConstants.DELETE)
                .children(
                        responseAssertion()
                                .fieldToTest(TargetField.RESPONSE_CODE)
                                .equalsToStrings(
                                        "204"));
    }
}
