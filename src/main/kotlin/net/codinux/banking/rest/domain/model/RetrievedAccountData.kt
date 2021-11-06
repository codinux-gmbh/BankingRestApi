package net.codinux.banking.rest.domain.model


class RetrievedAccountData(
  val account: BankAccount,
  val retrieveTransactions: RetrievedTransactions
)