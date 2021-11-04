package net.codinux.banking.rest.domain.model


open class GetAccountInfoParameter @JvmOverloads constructor(
  bankCode: String,
  loginName: String,
  password: String,
  /**
   * According to PSD2 for the account transactions of the last 90 days the two-factor authorization does not have to
   * be applied. It depends on the bank if they request a second factor (TAN) or not.
   *
   * So may the transactions can be retrieved without being asked for a TAN.
   *
   * If a TAN is required then the retrieving transactions process is aborted without casing the overall process to fail.
   */
  open val tryToRetrieveAccountTransactionOfLast90DaysWithoutTan: Boolean = false,
) : BankCredentials(bankCode, loginName, password) {

  @JvmOverloads
  constructor(credentials: BankCredentials, tryToRetrieveAccountTransactionOfLast90DaysWithoutTan: Boolean = false)
    : this(credentials.bankCode, credentials.loginName, credentials.password, tryToRetrieveAccountTransactionOfLast90DaysWithoutTan)

}