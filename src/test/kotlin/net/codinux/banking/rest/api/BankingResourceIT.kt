package net.codinux.banking.rest.api

import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import io.restassured.response.Response
import net.codinux.banking.rest.domain.model.BankCredentials
import org.hamcrest.CoreMatchers.*
import org.junit.jupiter.api.Test

import javax.inject.Inject

@QuarkusTest
class BankingResourceIT {

  companion object {
    private val bankCode = ""

    private val loginName = ""

    private val password = ""
  }


  @Inject
  internal lateinit var mapper: ObjectMapper


  @Test
  fun getAccountData() {

    post("account", BankCredentials(bankCode, loginName, password))
      .then()
        .log().all()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("successful", `is`(true))
        .body("error", nullValue())
        .body("data", not(nullValue()))
  }


  private fun post(endpoint: String, body: Any): Response {
    return given()
            .contentType(ContentType.JSON)
            .body(serialize(body))
          .`when`()
            .post("/banking/v1/" + endpoint)
  }

  private fun serialize(body: Any): String {
    return mapper.writeValueAsString(body)
  }

}