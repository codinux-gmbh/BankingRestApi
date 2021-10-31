package net.codinux.banking.rest.domain.clients.fints4k

import net.codinux.banking.rest.domain.model.*
import net.codinux.banking.rest.domain.model.BankData
import net.codinux.banking.rest.domain.util.TanMethodSelector
import net.dankito.banking.fints.callback.SimpleFinTsClientCallback
import net.dankito.banking.fints.model.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference


class fints4kBankingClient(
  private val bank: BankData,
  private val callback: BankingClientCallback
) : IBankingClient {

  private val mapper = fints4kModelMapper()

  private val mappedBank = mapper.map(bank)

  private val fintsCallback = SimpleFinTsClientCallback({ _, tanChallenge -> handleEnterTan(tanChallenge) }) { supportedTanMethods, suggestedTanMethod -> selectTanMethod(supportedTanMethods, suggestedTanMethod) }

  private val client = net.dankito.banking.fints.FinTsClientForCustomer(mappedBank, fintsCallback, SynchronousWebClient())

  private val tanMethodSelector = TanMethodSelector()


  override fun getAccountData(): Response<BankData> {
    val responseHolder = AsyncResponseHolder<BankData>()

    client.addAccountAsync(AddAccountParameter(mappedBank, false)) { response ->
      if (response.bank.accounts.isEmpty()) { // retrieving accounts failed means an error occurred
        responseHolder.setResponse(mapper.mapError(response))
      } else {
        responseHolder.setResponse(Response(mapper.map(bank, response.bank)))
      }
    }

    return responseHolder.waitForResponse()
  }

  override fun getTransactions(bank: BankData, config: GetAccountTransactionsConfig): Response<RetrievedAccountTransactions> {
    val responseHolder = AsyncResponseHolder<RetrievedAccountTransactions>()

    client.getTransactionsAsync(mapper.map(bank, config)) { response ->
      if (response.retrievedData.isEmpty() || response.retrievedData.first().successfullyRetrievedData == false) {
        responseHolder.setResponse(mapper.mapError(response))
      } else {
        responseHolder.setResponse(Response(mapper.map(response.retrievedData.first())))
      }
    }

    return responseHolder.waitForResponse()
  }


  private fun handleEnterTan(tanChallenge: TanChallenge): EnterTanResult {
    val responseHolder = AtomicReference<EnterTanResult>()
    val countDownLatch = CountDownLatch(1)

    callback.enterTan(bank, mapper.map(tanChallenge)) { enterTanResult ->
      responseHolder.set(mapper.map(enterTanResult))
      countDownLatch.countDown()
    }

    countDownLatch.await()

    return responseHolder.get()
  }

  private fun selectTanMethod(supportedTanMethods: List<TanMethod>, suggestedTanMethod: TanMethod?): TanMethod? {
    val selected = tanMethodSelector.selectNonVisual(mapper.map(supportedTanMethods))

    return selected?.let { supportedTanMethods.firstOrNull { it.securityFunction.code == selected.bankInternalMethodCode } } ?: suggestedTanMethod
  }

}