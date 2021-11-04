package net.codinux.banking.rest.domain.model


open class BankAccount(
    identifier: String,
    subAccountNumber: String?,
    iban: String?,
    val accountHolderName: String,
    val type: BankAccountType = BankAccountType.CheckingAccount,
    val productName: String? = null,
    val currency: String = "EUR",
    val accountLimit: String? = null,
    val supportsRetrievingTransactions: Boolean = false,
    val supportsRetrievingBalance: Boolean = false,
    val supportsTransferringMoney: Boolean = false,
    val supportsInstantPayment: Boolean = false
) : BankAccountIdentifier(identifier, subAccountNumber, iban) {

    internal constructor() : this("", null, null, "") // for object deserializers



    override fun toString(): String {
        return "$accountHolderName ($identifier)"
    }

}