package com.fedd.salesforce.services;

import us.abstracta.jmeter.javadsl.core.controllers.DslTransactionController;
import us.abstracta.jmeter.javadsl.http.DslHttpSampler;

/**
 * Service class for querying and deleting Salesforce Account records
 * using the JMeter Java DSL.
 *
 * <p>Accounts are created implicitly via Lead conversion, so this service
 * only provides read and delete operations. Inherits standard operations
 * from {@link AbstractSalesforceService}.</p>
 */
public class AccountService extends AbstractSalesforceService {

        @Override protected String sObjectName()       { return "Account"; }
        @Override protected String idVariable()         { return "accountId"; }
        @Override protected String currentIdVariable()  { return "currentAccountId"; }
        @Override protected String displayName()        { return "Account"; }

        /** @deprecated Use {@link #getByOwner()} instead. */
        public DslHttpSampler getAccounts() { return getByOwner(); }

        /** @deprecated Use {@link #getAll()} instead. */
        public DslHttpSampler getAllAccounts() { return getAll(); }

        /** @deprecated Use {@link #deleteRecord()} instead. */
        public DslHttpSampler deleteAccount() { return deleteRecord(); }

        /** @deprecated Use {@link #deleteAll()} instead. */
        public DslTransactionController deleteAllAccounts() { return deleteAll(); }
}
