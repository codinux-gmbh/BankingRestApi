package net.codinux.banking.rest.domain.model


enum class ErrorType {

  NoBankFoundForBankCode,

  BankDoesNotSupportFinTs3,

  WrongCredentials,

  TanRequiredButConfiguredToAbortThen,

  UserCancelledAction,

  InternalError

}