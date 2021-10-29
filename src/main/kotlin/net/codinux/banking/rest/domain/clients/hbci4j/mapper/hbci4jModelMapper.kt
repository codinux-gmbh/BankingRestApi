package net.codinux.banking.rest.domain.clients.hbci4j.mapper

import net.codinux.banking.rest.domain.model.BankAccount
import net.codinux.banking.rest.domain.model.BankAccountType
import net.codinux.banking.rest.domain.model.BankData
import net.codinux.banking.rest.domain.model.tan.TanMethod
import net.codinux.banking.rest.domain.model.tan.TanMethodType
import org.kapott.hbci.passport.HBCIPassport
import org.kapott.hbci.structures.Konto
import org.kapott.hbci.structures.Value
import java.math.BigDecimal


open class hbci4jModelMapper {


    open fun mapAccounts(bank: BankData, accounts: Array<out Konto>, passport: HBCIPassport): List<BankAccount> {
        return accounts.map { account ->
            val iban = if (account.iban.isNullOrBlank() == false) account.iban else passport.upd.getProperty("KInfo.iban") ?: ""

            return@map BankAccount(account.number,
                if (account.name2.isNullOrBlank() == false) account.name + " " + account.name2 else account.name,
            iban,
            account.subnumber,
            BigDecimal.ZERO,
            account.curr,
            mapBankAccountType(account),
            account.type ?: passport.upd.getProperty("KInfo.konto"),
            account.limit?.value?.let { mapValue(it).toString() },
            null,
            null,
            account.allowedGVs.contains("HKKAZ"),
            account.allowedGVs.contains("HKSAL"),
            account.allowedGVs.contains("HKCCS"),
            false // TODO: may implement real time transfer one day
            )
        }
    }

    open fun mapBankAccountType(account: Konto): BankAccountType {
        val type = account.acctype

        return when {
            type.length == 1 -> BankAccountType.CheckingAccount
            type.startsWith("1") -> BankAccountType.SavingsAccount
            type.startsWith("2") -> BankAccountType.FixedTermDepositAccount
            type.startsWith("3") -> BankAccountType.SecuritiesAccount
            type.startsWith("4") -> BankAccountType.LoanAccount
            type.startsWith("5") -> BankAccountType.CreditCardAccount
            type.startsWith("6") -> BankAccountType.FundDeposit
            type.startsWith("7") -> BankAccountType.BuildingLoanContract
            type.startsWith("8") -> BankAccountType.InsuranceContract
            type.startsWith("9") -> BankAccountType.Other
            else -> BankAccountType.Other
        }
    }

    protected open fun mapValue(value: Value): BigDecimal {
        return BigDecimal.valueOf(value.longValue).divide(BigDecimal.valueOf(100))
    }

    fun mapTanMethods(passport: HBCIPassport, tanMethodsString: String): List<TanMethod> {
        return tanMethodsString.split('|')
            .map { mapTanMethod(passport, it) }
            .filterNotNull()
    }

    fun mapTanMethod(passport: HBCIPassport, tanMethodString: String): TanMethod? {
        val parts = tanMethodString.split(':')

        if (parts.size > 1) {
            val code = parts[0]
            val displayName = parts[1]
            val displayNameLowerCase = displayName.toLowerCase()
            var maxTanInputLength: Int? = null
            var allowedTanFormat: AllowedTanFormat = AllowedTanFormat.Alphanumeric

            (passport as? AbstractPinTanPassport)?.twostepMechanisms?.get(code)?.let { tanMethodProperties ->
                maxTanInputLength = tanMethodProperties["maxlentan2step"]?.toString()?.toIntOrNull()
                if (tanMethodProperties["tanformat"] == "1") {
                    allowedTanFormat = AllowedTanFormat.Numeric
                }
            }

            return when {
                // TODO: implement all TAN methods
                displayNameLowerCase.contains("chiptan") -> {
                    if (displayNameLowerCase.contains("qr")) {
                        TanMethod(displayName, TanMethodType.ChipTanQrCode, code, maxTanInputLength, allowedTanFormat)
                    }
                    else {
                        TanMethod(displayName, TanMethodType.ChipTanFlickercode, code, maxTanInputLength, allowedTanFormat)
                    }
                }

                displayNameLowerCase.contains("sms") -> TanMethod(displayName, TanMethodType.SmsTan, code, maxTanInputLength, allowedTanFormat)
                displayNameLowerCase.contains("push") -> TanMethod(displayName, TanMethodType.AppTan, code, maxTanInputLength, allowedTanFormat)

                // we filter out iTAN and Einschritt-Verfahren as they are not permitted anymore according to PSD2
                else -> null
            }
        }

        return null
    }

}