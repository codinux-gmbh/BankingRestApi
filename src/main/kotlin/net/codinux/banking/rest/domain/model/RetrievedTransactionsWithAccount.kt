package net.codinux.banking.rest.domain.model

import com.fasterxml.jackson.annotation.JsonIgnore
import java.math.BigDecimal
import java.util.*


class RetrievedTransactionsWithAccount(
  @JsonIgnore
  val account: BankAccount,
  balance: BigDecimal?,
  bookedTransactions: List<AccountTransaction>,
//  unbookedTransactions: List<Any>,
  retrievedTransactionsFrom: Date?,
  retrievedTransactionsTo: Date?,
) : RetrievedTransactions(balance, bookedTransactions, retrievedTransactionsFrom, retrievedTransactionsTo)