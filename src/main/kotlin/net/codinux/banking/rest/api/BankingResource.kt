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
  @Path("accountinfo")
  @Operation(operationId = "getAccountInfo", summary = "Retrieve information for an account. Call this first to get the information to call the other APIs.",
    description = "Retrieves information for an account like its bank accounts (Girokonto, Kreditkartenkonto etc.), its supported TAN methods and so on.<br><br>" +
    "Additionally may also retrieves the transactions of the last 90 days, but only if no TAN is required for this.<br>" +
    "However, as retrieving transactions is only an optional step, the overall process is considered successful if retrieving account information succeeds, " +
      "no matter if retrieving transactions for one or multiple bank accounts fail.")
  @APIResponses(
    APIResponse(responseCode = "200", description = "No matter if bank data could successfully be retrieved, the credentials are wrong, a TAN is required or " +
      "any other error occurred, currently always response code 200 is returned with more details in the response."),
    APIResponse(responseCode = "500", description = "An internal error occurred. Please inform the developers")
  )
  fun getAccountInfo(param: GetAccountInfoParameter): Response<RetrievedAccountsData> {
    return service.getAccountInfo(param)
  }

  @POST
  @Path("bankaccountsdata")
  @Operation(operationId = "getBankAccountData", summary = "Retrieves data like account transactions (Kontoumsätze) and balance (Saldo) for a single bank account")
  @APIResponses(
    APIResponse(responseCode = "200", description = "No matter if account transactions could successfully be retrieved, the credentials are wrong, a TAN is required or " +
      "any other error occurred, currently always response code 200 is returned with more details in the response."),
    APIResponse(responseCode = "500", description = "An internal error occurred. Please inform the developers")
  )
  fun getBankAccountData(param: GetAccountDataParameter): RetrievedAccountData {
    return service.getAccountData(param)
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