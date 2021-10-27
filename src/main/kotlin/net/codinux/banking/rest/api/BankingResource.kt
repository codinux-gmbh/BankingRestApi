package net.codinux.banking.rest.api

import net.codinux.banking.rest.domain.BankingService
import net.codinux.banking.rest.domain.model.BankCredentials
import net.codinux.banking.rest.domain.model.BankData
import net.codinux.banking.rest.domain.model.Response
import javax.inject.Inject
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType


@Path("/banking/v1")
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

}