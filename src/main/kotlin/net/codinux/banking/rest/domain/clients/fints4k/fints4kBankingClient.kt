package net.codinux.banking.rest.domain.clients.fints4k

import net.codinux.banking.rest.domain.config.waitAtMaximumTillRequestTimeout
import net.codinux.banking.rest.domain.model.*
import net.codinux.banking.rest.domain.model.BankData
import net.dankito.banking.fints.callback.SimpleFinTsClientCallback
import net.dankito.banking.fints.model.*
import net.dankito.banking.fints.util.TanMethodSelector
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


  override fun getAccountInfo(): Response<BankData> {
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

  override fun getTransactions(bank: BankData, param: GetAccountDataParameter): Response<RetrievedTransactionsWithAccount> {
    val responseHolder = AsyncResponseHolder<RetrievedTransactionsWithAccount>()

    client.getTransactionsAsync(mapper.map(bank, param)) { response ->
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

    countDownLatch.waitAtMaximumTillRequestTimeout()

    return responseHolder.get() ?: EnterTanResult.userDidNotEnterTan()
  }

  private fun selectTanMethod(supportedTanMethods: List<TanMethod>, suggestedTanMethod: TanMethod?): TanMethod? {
    return tanMethodSelector.selectNonVisual(supportedTanMethods) ?: suggestedTanMethod
  }

}