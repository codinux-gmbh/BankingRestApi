package net.codinux.banking.rest.domain

import net.codinux.banking.rest.domain.clients.fints4k.fints4kBankingClient
import net.codinux.banking.rest.domain.clients.hbci4j.hbci4jBankingClient
import net.codinux.banking.rest.domain.model.*
import net.codinux.banking.rest.domain.model.tan.EnterTanResult
import net.codinux.banking.rest.domain.model.tan.TanChallenge
import net.codinux.banking.rest.domain.model.tan.TanRequired
import net.dankito.banking.bankfinder.InMemoryBankFinder
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

import javax.enterprise.context.ApplicationScoped
import kotlin.concurrent.thread


@ApplicationScoped
class BankingService {

  private val bankFinder = InMemoryBankFinder()

  // TODO: create clean up job for timed out TAN requests
  private val tanRequests = ConcurrentHashMap<String, EnterTanContext>()

  private val callCount = AtomicLong(0)



  fun getAccountInfo(param: GetAccountInfoParameter): Response<RetrievedAccountsData> {
    val findBankResult = findBank(param)

    if (findBankResult.foundBank == null) {
      return Response(findBankResult.error!!, findBankResult.errorType)
    }

    return executeRequestThatPotentiallyRequiresTan(findBankResult.foundBank) { client -> getAccountInfo(client, param) }
  }

  private fun getAccountInfo(client: IBankingClient, param: GetAccountInfoParameter): Response<RetrievedAccountsData> {
    val accountDataResponse = client.getAccountInfo()

    if (accountDataResponse.data != null && param.tryToRetrieveAccountTransactionOfLast90DaysWithoutTan) {
      val bank = accountDataResponse.data
      val getAccountsDataParam = GetAccountsDataParameter(param, bank.accounts, true, true, abortIfTanIsRequired = true)
      val accountsDataResult = getAccountsData(getAccountsDataParam, bank, bank.accounts)
      return Response(RetrievedAccountsData(bank, accountsDataResult))
    }

    return accountDataResponse as Response<RetrievedAccountsData>
  }


  fun getAccountData(param: GetAccountDataParameter): Response<RetrievedAccountData> {
    val account = BankAccount(param.account)
    val findBankResult = findBank(param.credentials)

    if (findBankResult.foundBank == null) {
      return Response(findBankResult.error!!, findBankResult.errorType)
    }

    return getAccountData(param, findBankResult.foundBank)
  }

  private fun getAccountData(param: GetAccountDataParameter, bank: BankData): Response<RetrievedAccountData> {
    val transactionsResponse = getAccountTransactions(param, bank)

    if (transactionsResponse.data != null) {
      return Response(RetrievedAccountData(transactionsResponse.data.account, transactionsResponse.data))
    }

    return transactionsResponse as Response<RetrievedAccountData>
  }

  private fun getAccountTransactions(param: GetAccountDataParameter, bank: BankData): Response<RetrievedTransactionsWithAccount> {
    if (param.getTransactionsOfLast90Days) {
      param.fromDate = calculate90DaysAgo()
    }

    return executeRequestThatPotentiallyRequiresTan(bank) { client -> client.getTransactions(bank, param) }
  }

  private fun calculate90DaysAgo(): Date {
    val calendar = Calendar.getInstance()
    calendar.time = Date()
    calendar.add(Calendar.DATE, -90)

    return Date(calendar.time.time)
  }


  private fun getAccountsData(param: GetAccountsDataParameter, bank: BankData, accounts: List<BankAccountIdentifier>): List<Response<RetrievedAccountData>> {
    val futures = accounts.map { account ->
      CompletableFuture.supplyAsync {
        val getDataParam = GetAccountDataParameter(param.credentials, account, param.alsoRetrieveBalance, param.getTransactionsOfLast90Days, param.fromDate, param.toDate, param.abortIfTanIsRequired)
        getAccountData(getDataParam, bank) }
    }

    CompletableFuture.allOf(*futures.toTypedArray()).get() // why does get() return null and not the result of the single futures (List<Response<RetrievedAccountData>>)?

    return futures.map { it.get() }
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
    return fints4kBankingClient(bank, callback)
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
    thread(name = "${bank.bankCode}_${bank.loginName}_${callCount.incrementAndGet()}") {
      val client = getClient(bank, callback)

      var response = executeRequest(client)

      if (response.error != null) { // in error case check if for this client a fallback exists that may can handle messages for this bank (like hbci4j for fints4k)
        hasFallbackClient(client, response, bank, callback)?.let { fallbackClient ->
          response = executeRequest(fallbackClient)
        }
      }

      responseHolder.setResponse(response)
    }

    return responseHolder.waitForResponse()
  }

  private fun <T> hasFallbackClient(client: IBankingClient, originalClientResponse: Response<T>, bank: BankData, callback: BankingClientCallback): IBankingClient? {
    if (client is fints4kBankingClient) { // fints4k is the first choose for banks supporting FinTS; if fints4k didn't work, we can try hbci4j
      if (originalClientResponse.error != null && originalClientResponse.errorType == null) { // but only if an error occurred but it's not an know error like wrong credentials or we were told to abort if TAN is required
        return hbci4jBankingClient(bank, callback)
      }
    }

    return null
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


  private fun findBank(credentials: BankCredentials): FindBankResult {
    val bankSearchResult = bankFinder.findBankByBankCode(credentials.bankCode)
    val potentialBankInfo = bankSearchResult.firstOrNull { it.pinTanAddress != null } ?: bankSearchResult.firstOrNull()

    if (potentialBankInfo == null) {
      return FindBankResult(null, "No bank found for bank code '${credentials.bankCode}'", ErrorType.NoBankFoundForBankCode)
    } else if (potentialBankInfo.pinTanAddress == null) {
      return FindBankResult(null, "Bank '${potentialBankInfo.name} does not support FinTS 3.0", ErrorType.BankDoesNotSupportFinTs3)
    }


    val bank = BankData(credentials.bankCode, credentials.loginName, credentials.password, potentialBankInfo.pinTanAddress ?: "",
      potentialBankInfo.name, potentialBankInfo.bic)

    return FindBankResult(bank, null, null)
  }

}