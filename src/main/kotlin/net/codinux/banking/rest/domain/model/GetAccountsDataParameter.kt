package net.codinux.banking.rest.domain.model

import java.util.*


class GetAccountsDataParameter(
    credentials: BankCredentials,
    /**
     * Optionally specify for which bank account to retrieve the account data.
     * If not set the data for all bank accounts of this account will be retrieved.
     */
    val accounts: List<BankAccountIdentifier>? = null,
    alsoRetrieveBalance: Boolean = true,
    getTransactionsOfLast90Days: Boolean = false,
    fromDate: Date? = null,
    toDate: Date? = null,
    abortIfTanIsRequired: Boolean = false
) : GetAccountDataParameterBase(credentials, alsoRetrieveBalance, getTransactionsOfLast90Days, fromDate, toDate, abortIfTanIsRequired)