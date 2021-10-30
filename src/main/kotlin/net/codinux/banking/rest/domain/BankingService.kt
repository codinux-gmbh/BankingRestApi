package net.codinux.banking.rest.domain

import net.codinux.banking.rest.domain.clients.hbci4j.hbci4jBankingClient
import net.codinux.banking.rest.domain.model.*
import net.dankito.banking.bankfinder.InMemoryBankFinder
import java.util.*

import javax.enterprise.context.ApplicationScoped


@ApplicationScoped
class BankingService {

  private val bankFinder = InMemoryBankFinder()


  fun getAccountData(credentials: BankCredentials): Response<BankData> {
    val (bank, errorMessage) = mapToBankData(credentials)

    if (errorMessage != null) {
      return Response(errorMessage)
    }

    return getClient(bank).getAccountData()
  }


  /**
   * According to PSD2 for the account transactions of the last 90 days the two-factor authorization does not have to
   * be applied. It depends on the bank if they request a second factor or not.
   *
   * So may this call succeeds without being asked for a TAN.
   */
  fun getAccountTransactionsOfLast90Days(config: GetAccountTransactionsConfig): Response<RetrievedAccountTransactions> {
    config.fromDate = calculate90DaysAgo()

    return getAccountTransactions(config)
  }

  fun getAccountTransactions(config: GetAccountTransactionsConfig): Response<RetrievedAccountTransactions> {
    val (bank, errorMessage) = mapToBankData(config.credentials)

    if (errorMessage != null) {
      return Response(errorMessage)
    }

    return getClient(bank).getTransactions(bank, config)
  }

  private fun calculate90DaysAgo(): Date {
    val calendar = Calendar.getInstance()
    calendar.time = Date()
    calendar.add(Calendar.DATE, -90)

    return Date(calendar.time.time)
  }


  private fun getClient(bank: BankData): IBankingClient {
    return hbci4jBankingClient(bank)
  }

  private fun mapToBankData(credentials: BankCredentials): Pair<BankData, String?> {
    val bankSearchResult = bankFinder.findBankByBankCode(credentials.bankCode)
    val fintsServerAddress = bankSearchResult.firstOrNull { it.pinTanAddress != null }?.pinTanAddress
    val potentialBankInfo = bankSearchResult.firstOrNull()

    val bank = BankData(credentials.bankCode, credentials.loginName, credentials.password, fintsServerAddress ?: "",
      potentialBankInfo?.name ?: "", potentialBankInfo?.bic ?: "")

    if (fintsServerAddress == null) {
      val errorMessage = if (bankSearchResult.isEmpty()) "No bank found for bank code '${credentials.bankCode}'" else "Bank '${bankSearchResult.firstOrNull()?.name} does not support FinTS 3.0"

      return Pair(bank, errorMessage)
    }

    return Pair(bank, null)
  }

}