package net.codinux.banking.rest.domain.model

import net.dankito.utils.multiplatform.Date
import java.math.BigDecimal


open class BankAccount(
    var identifier: String,
    var accountHolderName: String,
    var iban: String?,
    var subAccountNumber: String?,
    var balance: BigDecimal = BigDecimal.ZERO,
    var currency: String = "EUR",
    var type: BankAccountType = BankAccountType.CheckingAccount,
    var productName: String? = null,
    var accountLimit: String? = null,
    var retrievedTransactionsFromOn: Date? = null,
    var retrievedTransactionsUpTo: Date? = null,
    var supportsRetrievingAccountTransactions: Boolean = false,
    var supportsRetrievingBalance: Boolean = false,
    var supportsTransferringMoney: Boolean = false,
    var supportsRealTimeTransfer: Boolean = false,
) {

    internal constructor() : this("", "", null, null) // for object deserializers


    var countDaysForWhichTransactionsAreKept: Int? = null



    override fun toString(): String {
        return "$accountHolderName ($identifier)"
    }

}