package net.codinux.banking.rest.domain

import net.codinux.banking.rest.domain.clients.hbci4j.hbci4jBankingClient
import net.codinux.banking.rest.domain.model.*
import net.codinux.banking.rest.domain.model.tan.EnterTanResult
import net.codinux.banking.rest.domain.model.tan.TanChallenge
import net.codinux.banking.rest.domain.model.tan.TanRequired
import net.dankito.banking.bankfinder.InMemoryBankFinder
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

import javax.enterprise.context.ApplicationScoped
import kotlin.concurrent.thread


@ApplicationScoped
class BankingService {

  private val bankFinder = InMemoryBankFinder()

  // TODO: create clean up job for timed out TAN requests
  private val tanRequests = ConcurrentHashMap<String, EnterTanContext>()



  fun getAccountData(credentials: BankCredentials): Response<BankData> {
    val (bank, errorMessage) = mapToBankData(credentials)

    if (errorMessage != null) {
      return Response(errorMessage)
    }

    return executeRequestThatPotentiallyRequiresTan(bank) { client -> client.getAccountData() }
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

    return executeRequestThatPotentiallyRequiresTan(bank) { client -> client.getTransactions(bank, config) }
  }

  private fun calculate90DaysAgo(): Date {
    val calendar = Calendar.getInstance()
    calendar.time = Date()
    calendar.add(Calendar.DATE, -90)

    return Date(calendar.time.time)
  }


  fun handleTanResponse(tanRequestId: String, enterTanResult: EnterTanResult): Response<*> {
    tanRequests.remove(tanRequestId)?.let { enterTanContext ->
      val responseHolder = enterTanContext.responseHolder
      responseHolder.resetAfterEnteringTan()

      enterTanContext.enterTanResult.set(enterTanResult)
      enterTanContext.countDownLatch.countDown()

      return responseHolder.waitForResponse()
    }

    return Response<Any>("No TAN request found for TAN Request ID '$tanRequestId'")
  }


  private fun getClient(bank: BankData, callback: BankingClientCallback): IBankingClient {
    return hbci4jBankingClient(bank, callback)
  }


  private fun <T> executeRequestThatPotentiallyRequiresTan(bank: BankData, executeRequest: (IBankingClient) -> Response<T>): Response<T> {
    val responseHolder = AsyncResponseHolder<T>()

    val callback = object : BankingClientCallback {
      override fun enterTan(bank: BankData, tanChallenge: TanChallenge, callback: (EnterTanResult) -> Unit) {
        callback.invoke(handleEnterTan(tanChallenge, responseHolder))
      }

    }

    // there are two reasons why we create hbci4j client and execute request on an extra thread:
    // - hbci4j sets many variables on current thread, so we shouldn't re-use threads as when re-entering thread after TAN has been entered the thread specific variables may already have been set to another client
    // - if a TAN is required, this is signalled via a callback which is executed on the same thread. So we would wait here forever when callback gets fired
    thread {
      val client = getClient(bank, callback)

      responseHolder.setResponse(executeRequest(client))
    }

    return responseHolder.waitForResponse()
  }

  private fun <T> handleEnterTan(tanChallenge: TanChallenge, responseHolder: AsyncResponseHolder<T>): EnterTanResult {
    val enterTanResult = AtomicReference<EnterTanResult>()
    val enterTanLatch = CountDownLatch(1)

    val tanRequestId = UUID.randomUUID().toString()

    tanRequests.put(tanRequestId, EnterTanContext(enterTanResult, responseHolder, enterTanLatch))

    responseHolder.setResponse(Response(TanRequired(tanRequestId, tanChallenge)))

    enterTanLatch.await()

    return enterTanResult.get()
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