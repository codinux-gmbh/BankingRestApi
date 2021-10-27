package net.codinux.banking.rest.domain

import net.codinux.banking.rest.domain.clients.hbci4j.hbci4jBankingClient
import net.codinux.banking.rest.domain.model.BankCredentials
import net.codinux.banking.rest.domain.model.BankData
import net.codinux.banking.rest.domain.model.IBankingClient
import net.codinux.banking.rest.domain.model.Response
import net.dankito.banking.bankfinder.InMemoryBankFinder

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


  private fun getClient(bank: BankData): IBankingClient {
    return hbci4jBankingClient(bank)
  }

  private fun mapToBankData(accessData: BankCredentials): Pair<BankData, String?> {
    val bankSearchResult = bankFinder.findBankByBankCode(accessData.bankCode)
    val fintsServerAddress = accessData.finTsServerAddress ?: bankSearchResult.firstOrNull { it.pinTanAddress != null }?.pinTanAddress
    val potentialBankInfo = bankSearchResult.firstOrNull()

    val bank = BankData(accessData.bankCode, accessData.loginName, accessData.password, fintsServerAddress ?: "",
      potentialBankInfo?.name ?: "", potentialBankInfo?.bic ?: "")

    if (fintsServerAddress == null) {
      val errorMessage = if (bankSearchResult.isEmpty()) "No bank found for bank code '${accessData.bankCode}'" else "Bank '${bankSearchResult.firstOrNull()?.name} does not support FinTS 3.0"

      return Pair(bank, errorMessage)
    }

    return Pair(bank, null)
  }

}