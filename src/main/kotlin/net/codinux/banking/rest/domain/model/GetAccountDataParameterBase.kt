package net.codinux.banking.rest.domain.model

import java.util.*


open class GetAccountDataParameterBase(
    val credentials: BankCredentials,
    val alsoRetrieveBalance: Boolean = true,
    /**
     * If set overwrites the fromDate parameter.
     *
     * According to PSD2 for the account transactions of the last 90 days the two-factor authorization does not have to
     * be applied. It depends on the bank if they request a second factor (TAN) or not.
     *
     * So may the transactions can be retrieved without being asked for a TAN.
     */
    val getTransactionsOfLast90Days: Boolean = false,
    var fromDate: Date? = null, // TODO: use a better type, either long or string
    val toDate: Date? = null,
    // TODO: implement
    val abortIfTanIsRequired: Boolean = false
)