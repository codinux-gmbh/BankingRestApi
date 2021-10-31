package net.codinux.banking.rest.api

import net.codinux.banking.rest.domain.BankingService
import net.codinux.banking.rest.domain.model.*
import net.codinux.banking.rest.domain.model.tan.EnterTanResult
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
  fun getAccountData(credentials: BankCredentials): Response<BankData> {
    return service.getAccountData(credentials)
  }

  @POST
  @Path("transactions")
  fun getAccountTransactions(config: GetAccountTransactionsConfig): Response<RetrievedAccountTransactions> {
    if (config.getTransactionsOfLast90Days) {
      return service.getAccountTransactionsOfLast90Days(config)
    }

    return service.getAccountTransactions(config)
  }

  @POST
  @Path("tan/{tanRequestId}")
  fun postEnterTanResult(@PathParam("tanRequestId") tanRequestId: String, enterTanResult: EnterTanResult): Response<*> {
    return service.handleTanResponse(tanRequestId, enterTanResult)
  }

}