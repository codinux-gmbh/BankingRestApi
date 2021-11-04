package net.codinux.banking.rest.domain.clients.hbci4j.model

import org.kapott.hbci.GV.HBCIJob
import org.kapott.hbci.status.HBCIExecStatus
import org.kapott.hbci.structures.Konto


class GetAccountTransactionsResult(
  val account: Konto,
  val balanceJob: HBCIJob?,
  val accountTransactionsJob: HBCIJob,
  val status: HBCIExecStatus
)