package net.codinux.banking.rest.api

import net.codinux.banking.rest.domain.BankingService
import net.codinux.banking.rest.domain.model.*
import net.codinux.banking.rest.domain.model.tan.EnterTanResult
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses
import javax.inject.Inject
import javax.ws.rs.*
import javax.ws.rs.core.MediaType


@Path("/banking/v1-beta")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class BankingResource {

  @Inject
  lateinit var service: BankingService


  @POST
  @Path("account")
  @Operation(summary = "Retrieves the basic data to a bank account like its accounts (Girokonto, Kreditkartenkonto etc.), its supported TAN methods and so on")
  @APIResponses(
    APIResponse(responseCode = "200", description = "No matter if bank data could successfully be retrieved, the credentials are wrong, a TAN is required or " +
      "any other error occurred, currently always response code 200 is returned with more details in the response."),
    APIResponse(responseCode = "500", description = "An internal error occurred. Please inform the developers")
  )
  fun getAccountData(credentials: BankCredentials): Response<BankData> {
    return service.getAccountData(credentials)
  }

  @POST
  @Path("transactions")
  @Operation(summary = "Retrieves the account transactions (Kontoumsätze) for a bank account")
  @APIResponses(
    APIResponse(responseCode = "200", description = "No matter if account transactions could successfully be retrieved, the credentials are wrong, a TAN is required or " +
      "any other error occurred, currently always response code 200 is returned with more details in the response."),
    APIResponse(responseCode = "500", description = "An internal error occurred. Please inform the developers")
  )
  fun getAccountTransactions(config: GetAccountTransactionsConfig): Response<RetrievedAccountTransactions> {
    if (config.getTransactionsOfLast90Days) {
      return service.getAccountTransactionsOfLast90Days(config)
    }

    return service.getAccountTransactions(config)
  }

  @POST
  @Path("tan/{tanRequestId}")
  @Operation(summary = "If a previous operation returned that it requires a TAN, submit the entered TAN here. The response is the same as for the original operation.")
  @APIResponses(
    APIResponse(responseCode = "200", description = "The same data structure as for the original operation will be returned."),
    APIResponse(responseCode = "500", description = "An internal error occurred. Please inform the developers")
  )
  fun postEnterTanResult(@PathParam("tanRequestId") tanRequestId: String, enterTanResult: EnterTanResult): Response<*> {
    return service.handleTanResponse(tanRequestId, enterTanResult)
  }

}