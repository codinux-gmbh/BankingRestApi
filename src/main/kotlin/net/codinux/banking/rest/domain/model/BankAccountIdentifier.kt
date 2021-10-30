package net.codinux.banking.rest.domain.model


open class BankAccountIdentifier(
    val identifier: String,
    val subAccountNumber: String?,
    val iban: String?,
)