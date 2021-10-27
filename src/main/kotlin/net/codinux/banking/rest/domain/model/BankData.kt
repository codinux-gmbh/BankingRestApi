package net.codinux.banking.rest.domain.model

import net.codinux.banking.rest.domain.model.tan.TanMedium
import net.codinux.banking.rest.domain.model.tan.TanMethod


class BankData(
    var bankCode: String,
    var loginName: String,
    var password: String,
    var finTsServerAddress: String,
    var bankName: String,
    var bic: String
) {


    var customerName: String = ""
    var userId: String = loginName

    var accounts: List<BankAccount> = listOf()

    var supportedTanMethods: List<TanMethod> = listOf()
    var selectedTanMethod: TanMethod? = null
    var tanMedia: List<TanMedium> = listOf()


    override fun toString(): String {
        return "$bankName $loginName"
    }

}