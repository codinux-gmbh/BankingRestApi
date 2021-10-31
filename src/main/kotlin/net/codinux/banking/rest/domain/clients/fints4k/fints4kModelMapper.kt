package net.codinux.banking.rest.domain.clients.fints4k

import net.codinux.banking.rest.domain.model.*
import net.codinux.banking.rest.domain.model.AccountTransaction
import net.codinux.banking.rest.domain.model.BankData
import net.dankito.banking.fints.messages.datenelemente.abgeleiteteformate.Laenderkennzeichen
import net.dankito.banking.fints.messages.datenelemente.implementierte.tan.AllowedTanFormat
import net.dankito.banking.fints.messages.datenelemente.implementierte.tan.TanMedium
import net.dankito.banking.fints.messages.datenelemente.implementierte.tan.TanMediumStatus
import net.dankito.banking.fints.model.*
import net.dankito.banking.fints.response.client.FinTsClientResponse
import net.dankito.banking.fints.response.client.GetTransactionsResponse
import net.dankito.banking.fints.response.segments.AccountType
import net.dankito.utils.multiplatform.toDate


class fints4kModelMapper {

  fun map(bank: BankData): net.dankito.banking.fints.model.BankData {
    return net.dankito.banking.fints.model.BankData(bank.bankCode, bank.loginName, bank.password, bank.finTsServerAddress, bank.bic, bank.bankName)
  }

  fun map(bank: BankData, account: BankAccountIdentifier): AccountData {
    return AccountData(account.identifier, account.subAccountNumber, Laenderkennzeichen.Germany, bank.bankCode, account.iban, "", null, null, "", null, null, listOf())
  }

  fun map(accountData: RetrievedAccountData): RetrievedAccountTransactions {
    return RetrievedAccountTransactions(map(accountData.bookedTransactions), accountData.balance?.bigDecimal)
  }

  fun map(transactions: Collection<net.dankito.banking.fints.model.AccountTransaction>): List<AccountTransaction> {
    return transactions.map { map(it) }
  }

  fun map(transaction: net.dankito.banking.fints.model.AccountTransaction): AccountTransaction {
    return AccountTransaction(transaction.amount.bigDecimal, transaction.amount.currency.code, transaction.unparsedReference, transaction.bookingDate,
      transaction.otherPartyName, transaction.otherPartyBankCode, transaction.otherPartyAccountId, transaction.bookingText, transaction.valueDate,
      transaction.statementNumber, transaction.sequenceNumber, transaction.openingBalance?.bigDecimal, transaction.closingBalance?.bigDecimal,
      transaction.endToEndReference, transaction.customerReference, transaction.mandateReference, transaction.creditorIdentifier, transaction.originatorsIdentificationCode,
      transaction.compensationAmount, transaction.originalAmount, transaction.sepaReference, transaction.deviantOriginator, transaction.deviantRecipient,
      transaction.referenceWithNoSpecialType, transaction.primaNotaNumber, transaction.textKeySupplement,
      transaction.currencyType, transaction.bookingKey, transaction.referenceForTheAccountOwner, transaction.referenceOfTheAccountServicingInstitution, transaction.supplementaryDetails,
      transaction.transactionReferenceNumber, transaction.relatedReferenceNumber)
  }


  fun map(bank: BankData, fintsBank: net.dankito.banking.fints.model.BankData): BankData {
    bank.customerName = fintsBank.customerName
    bank.userId = fintsBank.userId
    bank.bankName = fintsBank.bankName
    bank.bic = fintsBank.bic
    bank.finTsServerAddress = fintsBank.finTs3ServerAddress

    bank.accounts = fintsBank.accounts.map { map(it) }
    bank.supportedTanMethods = fintsBank.tanMethodsAvailableForUser.map { map(it) }
    bank.selectedTanMethod = bank.supportedTanMethods.firstOrNull { it.bankInternalMethodCode == fintsBank.selectedTanMethod.securityFunction.code }
    bank.tanMedia = fintsBank.tanMedia.map { map(it) }

    return bank
  }

  private fun map(account: AccountData): BankAccount {
    return BankAccount(account.accountIdentifier, account.subAccountAttribute, account.iban, account.accountHolderName, account.currency ?: "EUR",
      map(account.accountType), account.productName, account.accountLimit,
      account.supportsRetrievingAccountTransactions, account.supportsRetrievingBalance, account.supportsTransferringMoney, account.supportsRealTimeTransfer)
  }

  private fun map(type: AccountType?): BankAccountType {
    return when (type) {
      AccountType.Girokonto -> BankAccountType.CheckingAccount
      AccountType.Sparkonto -> BankAccountType.SavingsAccount
      AccountType.Festgeldkonto -> BankAccountType.FixedTermDepositAccount
      AccountType.Wertpapierdepot -> BankAccountType.SecuritiesAccount
      AccountType.Darlehenskonto -> BankAccountType.LoanAccount
      AccountType.Kreditkartenkonto -> BankAccountType.CreditCardAccount
      AccountType.FondsDepot -> BankAccountType.FundDeposit
      AccountType.Bausparvertrag -> BankAccountType.BuildingLoanContract
      AccountType.Versicherungsvertrag -> BankAccountType.InsuranceContract
      else -> BankAccountType.Other
    }
  }


  fun map(challenge: TanChallenge): net.codinux.banking.rest.domain.model.tan.TanChallenge {
    return net.codinux.banking.rest.domain.model.tan.TanChallenge(challenge.messageToShowToUser, map(challenge.tanMethod))
  }

  fun map(tanMethods: List<TanMethod>): List<net.codinux.banking.rest.domain.model.tan.TanMethod> {
    return tanMethods.map { map(it) }
  }

  fun map(tanMethod: TanMethod): net.codinux.banking.rest.domain.model.tan.TanMethod {
    return net.codinux.banking.rest.domain.model.tan.TanMethod(tanMethod.displayName, map(tanMethod.type), tanMethod.securityFunction.code, tanMethod.maxTanInputLength, map(tanMethod.allowedTanFormat))
  }

  private fun map(type: TanMethodType): net.codinux.banking.rest.domain.model.tan.TanMethodType {
    return when (type) {
      TanMethodType.EnterTan -> net.codinux.banking.rest.domain.model.tan.TanMethodType.EnterTan
      TanMethodType.ChipTanManuell -> net.codinux.banking.rest.domain.model.tan.TanMethodType.ChipTanManuell
      TanMethodType.ChipTanFlickercode -> net.codinux.banking.rest.domain.model.tan.TanMethodType.ChipTanFlickercode
      TanMethodType.ChipTanUsb -> net.codinux.banking.rest.domain.model.tan.TanMethodType.ChipTanUsb
      TanMethodType.ChipTanQrCode -> net.codinux.banking.rest.domain.model.tan.TanMethodType.ChipTanQrCode
      TanMethodType.ChipTanPhotoTanMatrixCode -> net.codinux.banking.rest.domain.model.tan.TanMethodType.ChipTanPhotoTanMatrixCode
      TanMethodType.SmsTan -> net.codinux.banking.rest.domain.model.tan.TanMethodType.SmsTan
      TanMethodType.AppTan -> net.codinux.banking.rest.domain.model.tan.TanMethodType.AppTan
      TanMethodType.photoTan -> net.codinux.banking.rest.domain.model.tan.TanMethodType.photoTan
      TanMethodType.QrCode -> net.codinux.banking.rest.domain.model.tan.TanMethodType.QrCode
    }
  }

  private fun map(format: AllowedTanFormat?): net.codinux.banking.rest.domain.model.tan.AllowedTanFormat {
    return when (format) {
      AllowedTanFormat.Numeric -> net.codinux.banking.rest.domain.model.tan.AllowedTanFormat.Numeric
      else -> net.codinux.banking.rest.domain.model.tan.AllowedTanFormat.Alphanumeric
    }
  }

  fun map(enterTanResult: net.codinux.banking.rest.domain.model.tan.EnterTanResult): EnterTanResult? {
    enterTanResult.enteredTan?.let {
      return EnterTanResult.userEnteredTan(it)
    }

    // TODO: also map changeTanMethodTo and changeTanMediumTo

    return EnterTanResult.userDidNotEnterTan()
  }

  private fun map(tanMedium: TanMedium): net.codinux.banking.rest.domain.model.tan.TanMedium {
    return net.codinux.banking.rest.domain.model.tan.TanMedium(tanMedium.mediumName ?: "", map(tanMedium.status))
  }

  private fun map(status: TanMediumStatus): net.codinux.banking.rest.domain.model.tan.TanMediumStatus {
    return when (status) {
      TanMediumStatus.Aktiv -> net.codinux.banking.rest.domain.model.tan.TanMediumStatus.Used
      else -> net.codinux.banking.rest.domain.model.tan.TanMediumStatus.Available
    }
  }


  fun map(bank: BankData, config: GetAccountTransactionsConfig): GetTransactionsParameter {
    val account = map(bank, config.account)

    return GetTransactionsParameter(account, config.alsoRetrieveBalance, config.fromDate?.toDate(), config.toDate?.toDate(), abortIfTanIsRequired = config.abortIfTanIsRequired)
  }


  fun <T> mapError(response: FinTsClientResponse): Response<T> {
    return Response(mapErrors(response) ?: "")
  }

  fun mapErrors(response: FinTsClientResponse): String? {
    return response.errorMessage ?:
    if (response.errorsToShowToUser.isEmpty()) null else response.errorsToShowToUser.joinToString("\n") // TODO: find a better way to choose which of these error messages to show
  }

}