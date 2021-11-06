package net.codinux.banking.rest.domain.model


class RetrievedAccountsData(
  val bank: BankData,
  val accountsData: List<Response<RetrievedAccountData>>
)