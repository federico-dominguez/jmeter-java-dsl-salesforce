package com.fedd.salesforce.services;

import static us.abstracta.jmeter.javadsl.JmeterDsl.httpSampler;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jsonAssertion;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jsonExtractor;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jsr223PostProcessor;
import static us.abstracta.jmeter.javadsl.JmeterDsl.regexExtractor;
import static us.abstracta.jmeter.javadsl.JmeterDsl.responseAssertion;

import com.fedd.salesforce.config.TestConfig;

import org.apache.http.entity.ContentType;

import us.abstracta.jmeter.javadsl.core.controllers.DslTransactionController;
import us.abstracta.jmeter.javadsl.core.postprocessors.DslJsonExtractor.JsonQueryLanguage;
import us.abstracta.jmeter.javadsl.http.DslHttpSampler;

/**
 * Service class for creating, querying, converting, and deleting Salesforce Lead records
 * using the JMeter Java DSL.
 *
 * <p>Inherits standard GET, DELETE, and bulk-delete operations from
 * {@link AbstractSalesforceService}. Adds Lead-specific operations:
 * create, convert (via SOAP API), and parameterized email generation.</p>
 */
public class LeadService extends AbstractSalesforceService {

  @Override protected String sObjectName()       { return "Lead"; }
  @Override protected String idVariable()         { return "leadId"; }
  @Override protected String currentIdVariable()  { return "currentLeadId"; }
  @Override protected String displayName()        { return "Lead"; }

  /** @deprecated Use {@link #getByOwner()} instead. */
  public DslHttpSampler getLeads() { return getByOwner(); }

  /** @deprecated Use {@link #getAll()} instead. */
  public DslHttpSampler getAllLeads() { return getAll(); }

  /** @deprecated Use {@link #deleteRecord()} instead. */
  public DslHttpSampler deleteLead() { return deleteRecord(); }

  /**
   * Creates a new Lead record using CSV-parameterized data.
   *
   * @return an HTTP sampler that POSTs a Lead and validates the response
   */
  public DslHttpSampler createLead() {
    return httpSampler("CREATE New Lead", TestConfig.restUrl("Lead"))
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
            jsonExtractor("leadId", "id")
                .defaultValue("leadId_NOT_FOUND"),
            jsonAssertion("Success Assertion", "$.success")
                .queryLanguage(JsonQueryLanguage.JSON_PATH)
                .equalsToJson("true"));
  }

  /**
   * Converts a Lead to an Account + Contact + Opportunity via the Salesforce SOAP API.
   * Stores the resulting Account, Opportunity, and Contact IDs as JMeter properties
   * for downstream use.
   *
   * @return an HTTP sampler that sends a SOAP convertLead request
   */
  public DslHttpSampler convertLead() {
    return httpSampler("UPDATE Lead to Close - Converted", TestConfig.soapUrl())
        .post(
            """
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
        .header("SOAPAction", "\"\"")
        .children(
            regexExtractor("accountId", "<accountId>(.+?)</accountId>"),
            regexExtractor("opportunityId", "<opportunityId>(.+?)</opportunityId>"),
            regexExtractor("contactId", "<contactId>(.+?)</contactId>"),
            jsr223PostProcessor("RECORD_ID Properties",
                "props.put('ACCOUNT_ID', vars.get('accountId')); "
                    + "props.put('OPPORTUNITY_ID', vars.get('opportunityId')); "
                    + "props.put('CONTACT_ID', vars.get('contactId'));"),
            responseAssertion("Success Assertion")
                .containsSubstrings("<success>true</success>"));
  }

  /** @deprecated Use {@link #deleteAll()} instead. */
  public DslTransactionController deleteAllLeads() { return deleteAll(); }
}
