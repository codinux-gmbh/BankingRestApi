package net.codinux.banking.rest.domain.model

import java.util.*


class GetAccountDataParameter(
    credentials: BankCredentials,
    /**
     * The identifier of the bank account for which to retrieve the data.
     */
    val account: BankAccountIdentifier,
    alsoRetrieveBalance: Boolean = true,
    getTransactionsOfLast90Days: Boolean = false,
    fromDate: Date? = null,
    toDate: Date? = null,
    abortIfTanIsRequired: Boolean = false
) : GetAccountDataParameterBase(credentials, alsoRetrieveBalance, getTransactionsOfLast90Days, fromDate, toDate, abortIfTanIsRequired)