package net.codinux.banking.rest.domain.model


class FindBankResult(
  val foundBank: BankData?,
  val error: String?,
  val errorType: ErrorType?
)