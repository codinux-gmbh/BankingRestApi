package net.codinux.banking.rest.domain.clients.hbci4j.mapper

import net.codinux.banking.rest.domain.model.*
import net.codinux.banking.rest.domain.model.tan.AllowedTanFormat
import net.codinux.banking.rest.domain.model.tan.TanMethod
import net.codinux.banking.rest.domain.model.tan.TanMethodType
import net.dankito.banking.fints.transactions.mt940.Mt940Parser
import org.kapott.hbci.GV_Result.GVRKUms
import org.kapott.hbci.passport.AbstractPinTanPassport
import org.kapott.hbci.passport.HBCIPassport
import org.kapott.hbci.structures.Konto
import org.kapott.hbci.structures.Value
import java.math.BigDecimal


class hbci4jModelMapper {

    fun mapToKonto(bank: BankData, account: BankAccountIdentifier): Konto {
        val konto = Konto("DE", bank.bankCode, account.identifier, account.subAccountNumber)

        konto.name = bank.bankName
        konto.iban = account.iban
        konto.bic = bank.bic

        return konto
    }



    fun mapAccounts(accounts: Array<out Konto>, passport: HBCIPassport): List<BankAccount> {
        return accounts.map { account ->
            val iban = if (account.iban.isNullOrBlank() == false) account.iban else passport.upd.getProperty("KInfo.iban") ?: ""
            val accountHolderName = if (account.name2.isNullOrBlank() == false) account.name + " " + account.name2 else account.name
            val productName = account.type ?: passport.upd.getProperty("KInfo.konto")

            return@map BankAccount(account.number,
                account.subnumber,
                iban,
                accountHolderName,
                BigDecimal.ZERO,
                account.curr,
                mapBankAccountType(account),
                productName,
                account.limit?.value?.let { mapValue(it).toString() },
                account.allowedGVs.contains("HKKAZ"),
                account.allowedGVs.contains("HKSAL"),
                account.allowedGVs.contains("HKCCS"),
                false // TODO: may implement real time transfer one day
            )
        }
    }

    private fun mapBankAccountType(account: Konto): BankAccountType {
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


    fun mapTanMethods(passport: AbstractPinTanPassport): List<TanMethod> {
        return passport.allowedTwostepMechanisms.mapNotNull { mapTanMethod(passport, it) }
    }

    private fun mapTanMethod(passport: AbstractPinTanPassport, tanMethodCode: String): TanMethod? {

        passport.twostepMechanisms[tanMethodCode]?.let { tanMethodProperties ->
            val displayName = tanMethodProperties["name"] as String
            return mapTanMethod(passport, tanMethodCode, displayName)
        }

        return null
    }

    private fun mapTanMethod(passport: AbstractPinTanPassport, tanMethodCode: String, displayName: String): TanMethod? {
        val type = mapToTanMethodType(displayName)
        if (type != null) {

            var maxTanInputLength: Int? = null
            var allowedTanFormat: AllowedTanFormat = AllowedTanFormat.Alphanumeric

            passport.twostepMechanisms?.get(tanMethodCode)?.let { tanMethodProperties ->
                maxTanInputLength = tanMethodProperties["maxlentan2step"]?.toString()?.toIntOrNull()
                if (tanMethodProperties["tanformat"] == "1") {
                    allowedTanFormat = AllowedTanFormat.Numeric
                }
            }

            return TanMethod(displayName, type, tanMethodCode, maxTanInputLength, allowedTanFormat)
        }

        return null
    }

    // TODO: also check DkTanMethod and technicalTanMethodIdentification, see fints4k ModelMapper.mapToTanMethodType()
    private fun mapToTanMethodType(methodName: String): TanMethodType? {
        val name = methodName.toLowerCase()

        return when {
            // names are like 'chipTAN (comfort) manuell', 'Smart(-)TAN plus (manuell)' and
            // technical identification is 'HHD'. Exception:  there's one that states itself as 'chipTAN (Manuell)'
            // but its DkTanMethod is set to 'HHDOPT1' -> handle ChipTanManuell before ChipTanFlickercode
            name.contains("manuell") ->
                TanMethodType.ChipTanManuell

            // names are like 'chipTAN optisch/comfort', 'SmartTAN (plus) optic/USB', 'chipTAN (Flicker)' and
            // technical identification is 'HHDOPT1'
            tanMethodNameContains(name, "optisch", "optic", "comfort", "flicker") ->
                TanMethodType.ChipTanFlickercode

            // 'Smart-TAN plus optisch / USB' seems to be a Flickertan method -> test for 'optisch' first
            name.contains("usb") -> TanMethodType.ChipTanUsb

            // QRTAN+ from 1822 direct has nothing to do with chipTAN QR.
            name.contains("qr") -> {
                if (tanMethodNameContains(name, "chipTAN", "Smart")) TanMethodType.ChipTanQrCode
                else TanMethodType.QrCode
            }

            // photoTAN from Commerzbank (comdirect), Deutsche Bank, norisbank has nothing to do with chipTAN photo
            name.contains("photo") -> {
                // e.g. 'Smart-TAN photo' / description 'Challenge'
                if (tanMethodNameContains(name, "chipTAN", "Smart")) TanMethodType.ChipTanPhotoTanMatrixCode
                // e.g. 'photoTAN-Verfahren', description 'Freigabe durch photoTAN'
                else TanMethodType.photoTan
            }

            tanMethodNameContains(name, "SMS", "mobile", "mTAN") -> TanMethodType.SmsTan

            // 'flateXSecure' identifies itself as 'PPTAN' instead of 'AppTAN'
            // 'activeTAN-Verfahren' can actually be used either with an app or a reader; it's like chipTAN QR but without a chip card
            tanMethodNameContains(name, "push", "app", "BestSign", "SecureGo", "TAN2go", "activeTAN", "easyTAN", "SecurePlus", "TAN+")
              /*|| technicalTanMethodIdentificationContains(parameters, "SECURESIGN", "PPTAN")*/ ->
                TanMethodType.AppTan

            // we filter out iTAN and Einschritt-Verfahren as they are not permitted anymore according to PSD2
            else -> null
        }
    }

    private fun tanMethodNameContains(name: String, vararg namesToTest: String): Boolean {
        namesToTest.forEach { nameToTest ->
            if (name.contains(nameToTest.toLowerCase())) {
                return true
            }
        }

        return false
    }


    fun mapTransactions(result: GVRKUms): List<AccountTransaction> {
        val entries = mutableListOf<AccountTransaction>()

        result.dataPerDay.forEach { btag ->
            btag.lines.forEach { transaction ->
                entries.add(mapTransaction(btag, transaction))
            }
        }

        return entries
    }

    private fun mapTransaction(btag: GVRKUms.BTag, transaction: GVRKUms.UmsLine): AccountTransaction {
        val unparsedReference = transaction.usage.joinToString("")
        val parsedReference = Mt940Parser().getReferenceParts(unparsedReference)
        val statementAndMaySequenceNumber = btag.counter.split('/')

        return AccountTransaction(
            mapValue(transaction.value), transaction.value.curr, unparsedReference, transaction.bdate,
            transaction.other.name + (transaction.other.name2 ?: ""),
            transaction.other.bic ?: transaction.other.blz,
            transaction.other.iban ?: transaction.other.number,
            transaction.text, transaction.valuta,
            statementAndMaySequenceNumber[0].toInt(),
            if (statementAndMaySequenceNumber.size > 1) statementAndMaySequenceNumber[1].toInt() else null,
            mapValue(btag.start.value), mapValue(btag.end.value),

            parsedReference[Mt940Parser.EndToEndReferenceKey],
            parsedReference[Mt940Parser.CustomerReferenceKey],
            parsedReference[Mt940Parser.MandateReferenceKey],
            parsedReference[Mt940Parser.CreditorIdentifierKey],
            parsedReference[Mt940Parser.OriginatorsIdentificationCodeKey],
            parsedReference[Mt940Parser.CompensationAmountKey],
            parsedReference[Mt940Parser.OriginalAmountKey],
            parsedReference[Mt940Parser.SepaReferenceKey],
            parsedReference[Mt940Parser.DeviantOriginatorKey],
            parsedReference[Mt940Parser.DeviantRecipientKey],
            parsedReference[""],
            transaction.primanota,
            transaction.addkey,

            null,
            "",
            transaction.customerref,
            transaction.instref,
            transaction.additional,

            "",
            null
        )
    }

    private fun mapValue(value: Value): BigDecimal {
        return BigDecimal(BigDecimal.valueOf(value.longValue).divide(BigDecimal.valueOf(100)).toPlainString())
    }

}