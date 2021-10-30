package net.codinux.banking.rest.domain.model


interface IBankingClient {

  fun getAccountData(): Response<BankData>

  fun getTransactions(bank: BankData, config: GetAccountTransactionsConfig): Response<RetrievedAccountTransactions>

}