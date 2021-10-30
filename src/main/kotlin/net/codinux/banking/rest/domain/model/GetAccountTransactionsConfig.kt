package net.codinux.banking.rest.domain.model

import java.util.*


class GetAccountTransactionsConfig(
    val credentials: BankCredentials,
    val account: BankAccountIdentifier,
    val alsoRetrieveBalance: Boolean = true,
    var fromDate: Date? = null, // TODO: use a better type, either long or string
    val toDate: Date? = null,
    val abortIfTanIsRequired: Boolean = false
)