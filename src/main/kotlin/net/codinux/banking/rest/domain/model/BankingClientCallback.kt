package net.codinux.banking.rest.domain.model

import net.codinux.banking.rest.domain.model.tan.EnterTanResult
import net.codinux.banking.rest.domain.model.tan.TanChallenge


interface BankingClientCallback {

    fun enterTan(bank: BankData, tanChallenge: TanChallenge, callback: (EnterTanResult) -> Unit)

}