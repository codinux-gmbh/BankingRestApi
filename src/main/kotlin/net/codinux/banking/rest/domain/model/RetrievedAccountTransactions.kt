package net.codinux.banking.rest.domain.model

import java.math.BigDecimal


class RetrievedAccountTransactions(
  val transactions: List<AccountTransaction>,
  val balance: BigDecimal? = null
)