package net.codinux.banking.rest.api

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import io.restassured.response.Response
import net.codinux.banking.rest.domain.model.*
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.*
import org.junit.jupiter.api.Test

@QuarkusTest
class BankingResourceIT {

  companion object {
    private val bankCode = ""

    private val loginName = ""

    private val password = ""
  }


  @Test
  fun getAccountData() {

    val bankData = postAndValidate("account", BankCredentials(bankCode, loginName, password), BankData::class.java)

    assertThat(bankData.bankCode).isEqualTo(bankCode)
    assertThat(bankData.loginName).isEqualTo(loginName)
    assertThat(bankData.accounts).isNotEmpty
    assertThat(bankData.supportedTanMethods).isNotEmpty
  }


  private fun post(endpoint: String, body: Any): Response {
    return given()
            .contentType(ContentType.JSON)
            .body(body)
          .`when`()
            .post("/banking/v1/" + endpoint)
  }

  private fun <T> postAndValidate(endpoint: String, body: Any, responseClass: Class<T>): T {
    val response = post(endpoint, body)

    response.then()
      .log().all()
      .statusCode(200)
      .contentType(ContentType.JSON)
      .body("successful", `is`(true))
      .body("error", nullValue())
      .body("data", not(nullValue()))

    return response.jsonPath().getObject("data", responseClass)
  }

}