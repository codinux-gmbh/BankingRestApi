package net.codinux.banking.rest.domain.model


open class GetAccountInfoParameter(
  bankCode: String,
  loginName: String,
  password: String
) : BankCredentials(bankCode, loginName, password) {

  constructor(credentials: BankCredentials) : this(credentials.bankCode, credentials.loginName, credentials.password)

}