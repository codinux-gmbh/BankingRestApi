package net.codinux.banking.rest.domain.model


interface IBankingClient {

  fun getAccountData(): Response<BankData>

}