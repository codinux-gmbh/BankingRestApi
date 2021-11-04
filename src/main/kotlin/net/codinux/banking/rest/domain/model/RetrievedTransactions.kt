package net.codinux.banking.rest.domain.model

import java.math.BigDecimal
import java.util.*


open class RetrievedTransactions(
  val balance: BigDecimal?,
  val bookedTransactions: List<AccountTransaction>,
//  val unbookedTransactions: List<Any>,
  val retrievedTransactionsFrom: Date?, // TODO: find a better data type
  val retrievedTransactionsTo: Date?,
)