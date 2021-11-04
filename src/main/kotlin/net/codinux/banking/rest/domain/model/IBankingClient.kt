package net.codinux.banking.rest.domain.model


interface IBankingClient {

  fun getAccountInfo(): Response<BankData>

  fun getTransactions(bank: BankData, param: GetAccountDataParameter): Response<RetrievedTransactionsWithAccount>

}