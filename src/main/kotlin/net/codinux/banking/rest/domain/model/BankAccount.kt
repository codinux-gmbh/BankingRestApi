package net.codinux.banking.rest.domain.model

import java.math.BigDecimal


open class BankAccount(
    identifier: String,
    subAccountNumber: String?,
    iban: String?,
    val accountHolderName: String,
    val balance: BigDecimal = BigDecimal.ZERO,
    val currency: String = "EUR",
    val type: BankAccountType = BankAccountType.CheckingAccount,
    val productName: String? = null,
    val accountLimit: String? = null,
    val supportsRetrievingAccountTransactions: Boolean = false,
    val supportsRetrievingBalance: Boolean = false,
    val supportsTransferringMoney: Boolean = false,
    val supportsRealTimeTransfer: Boolean = false
) : BankAccountIdentifier(identifier, subAccountNumber, iban) {

    internal constructor() : this("", null, null, "") // for object deserializers



    override fun toString(): String {
        return "$accountHolderName ($identifier)"
    }

}