package net.codinux.banking.rest.api

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import io.restassured.response.Response
import net.codinux.banking.rest.domain.model.*
import net.codinux.banking.rest.domain.model.tan.EnterTanResult
import net.codinux.banking.rest.domain.model.tan.TanRequired
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@QuarkusTest
class BankingResourceIT {

  companion object {
    // TODO: set your values here:

    private val bankCode = ""

    private val loginName = ""

    private val password = ""

    // below values are needed to get account transactions and to transfer money
    private val accountIdentifier = ""

    private val accountSubAccountNumber: String? = null

    private val accountIban = ""
  }


  @Test
  fun getAccountInfo() {

    val result = postAndValidateSuccessful(Urls.AccountInfoSubPath, GetAccountInfoParameter(bankCode, loginName, password), BankData::class.java)

    assertThat(result.bankCode).isEqualTo(bankCode)
    assertThat(result.loginName).isEqualTo(loginName)
    assertThat(result.accounts).isNotEmpty
    assertThat(result.tanMethods).isNotEmpty
  }

  /**
   * In most cases retrieving account transactions of last 90 days works without having to enter a TAN.
   */
  @Test
  fun getAccountDataOfLast90Days() {
    val config = GetAccountDataParameter(getCredentials(), getBankAccountIdentifier(), getTransactionsOfLast90Days = true)

    val result = postAndValidateSuccessfulRetrievedAccountData(Urls.BankAccountDataSubPath, config)
  }

  @Disabled // not an automatic test, requires manually entering a TAN
  @Test
  fun getAccountData() {
    val config = GetAccountDataParameter(getCredentials(), getBankAccountIdentifier())

    val tanRequiredResult = postAndValidateTanRequired(Urls.BankAccountDataSubPath, config)

    val tanChallenge = tanRequiredResult.tanChallenge // just that you can better see it in debug window

    val enteredTan: String? = null


    // TODO: if you a TAN method that does not required data from tanChallenge above like AppTan or SmsTan, create a debugging break point below,
    // get the TAN and then set enteredTan variable in debug window with 'Set variable...'

    val transactionsResult = postAndValidateSuccessfulRetrievedAccountData("tan/${tanRequiredResult.tanRequestId}", EnterTanResult(enteredTan))
  }

  @Test
  fun getAccountsDataOfLast90Days() {
    val parameter = GetAccountsDataParameter(getCredentials(), getTransactionsOfLast90Days = true)

    val accountsData = postAndValidateSuccessful(Urls.BankAccountsDataSubPath, parameter, RetrievedAccountsData::class.java)

    assertThat(accountsData.bank.bankCode).isEqualTo(bankCode)
    assertThat(accountsData.bank.loginName).isEqualTo(loginName)
    assertThat(accountsData.bank.accounts).isNotEmpty
    assertThat(accountsData.bank.tanMethods).isNotEmpty
    
    assertThat(accountsData.accountsData).isNotEmpty()
  }


  private fun getCredentials() = BankCredentials(bankCode, loginName, password)

  private fun getBankAccountIdentifier() = BankAccountIdentifier(accountIdentifier, accountSubAccountNumber, accountIban)


  private fun post(endpoint: String, body: Any): Response {
    return given()
            .contentType(ContentType.JSON)
            .body(body)
          .`when`()
            .post(Urls.BaseUrl + endpoint)
  }

  private fun <T> postAndValidateSuccessful(endpoint: String, body: Any, responseClass: Class<T>): T {
    val response = postAndValidateBasicData(endpoint, body)

    response.then()
      .body("type", `is`("Success"))
      .body("error", nullValue())
      .body("errorType", nullValue())
      .body("data", not(nullValue()))
      .body("tanRequired", nullValue())

    return response.jsonPath().getObject("data", responseClass)
  }

  private fun postAndValidateSuccessfulRetrievedAccountData(endpoint: String, body: Any): RetrievedTransactions {
    val response = postAndValidateBasicData(endpoint, body)

    response.then()
      .body("type", `is`("Success"))
      .body("error", nullValue())
      .body("errorType", nullValue())
      .body("data", not(nullValue()))
      .body("tanRequired", nullValue())
      .body("data.account", not(nullValue()))
      .body("data.retrieveTransactions", not(nullValue()))


    val result = response.jsonPath().getObject("data.retrieveTransactions", RetrievedTransactions::class.java)

    assertThat(result).isNotNull()
    assertThat(result.balance).isNotNull()
    assertThat(result.retrievedTransactionsFrom).isNotNull()
    assertThat(result.retrievedTransactionsTo).isNotNull()
    assertThat(result.bookedTransactions).isNotEmpty

    return result
  }

  private fun postAndValidateTanRequired(endpoint: String, body: Any): TanRequired {
    val response = postAndValidateBasicData(endpoint, body)

    response.then()
      .body("type", `is`("TanRequired"))
      .body("error", nullValue())
      .body("errorType", nullValue())
      .body("data", nullValue())
      .body("tanRequired", not(nullValue()))

    return response.jsonPath().getObject("tanRequired", TanRequired::class.java)
  }

  private fun postAndValidateBasicData(endpoint: String, body: Any): Response {
    val response = post(endpoint, body)

    response.then()
      .log().all()
      .statusCode(200)
      .contentType(ContentType.JSON)
      .body("error", nullValue())

    return response
  }

}