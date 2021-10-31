package net.codinux.banking.rest.domain.model

import com.fasterxml.jackson.annotation.JsonInclude
import java.math.BigDecimal
import java.text.DateFormat
import java.util.*


@JsonInclude(JsonInclude.Include.NON_DEFAULT)
open class AccountTransaction(
    val amount: BigDecimal,
    @JsonInclude // don't know why Jackson removes this value during serialization
    val currency: String,
    @JsonInclude // unparsedReference may be an empty string (currently known only Postbank)
    val unparsedReference: String,
    val bookingDate: Date,
    val otherPartyName: String?,
    val otherPartyBankCode: String?,
    val otherPartyAccountId: String?,
    val bookingText: String?,
    val valueDate: Date,
    val statementNumber: Int,
    val sequenceNumber: Int?,
    val openingBalance: BigDecimal?,
    val closingBalance: BigDecimal?,

    val endToEndReference: String?,
    val customerReference: String?,
    val mandateReference: String?,
    val creditorIdentifier: String?,
    val originatorsIdentificationCode: String?,
    val compensationAmount: String?,
    val originalAmount: String?,
    val sepaReference: String?,
    val deviantOriginator: String?,
    val deviantRecipient: String?,
    val referenceWithNoSpecialType: String?,
    val primaNotaNumber: String?,
    val textKeySupplement: String?,

    val currencyType: String?,
    val bookingKey: String,
    val referenceForTheAccountOwner: String,
    val referenceOfTheAccountServicingInstitution: String?,
    val supplementaryDetails: String?,

    val transactionReferenceNumber: String,
    val relatedReferenceNumber: String?
) {

    internal constructor() : this(null, "", BigDecimal.ZERO, Date(), null) // for object deserializers

    constructor(otherPartyName: String?, unparsedReference: String, amount: BigDecimal, valueDate: Date, bookingText: String?)
            : this(amount, "EUR", unparsedReference, valueDate,
        otherPartyName, null, null, bookingText, valueDate)


    constructor(amount: BigDecimal, currency: String, unparsedReference: String, bookingDate: Date,
                otherPartyName: String?, otherPartyBankCode: String?, otherPartyAccountId: String?,
                bookingText: String?, valueDate: Date)
            : this(amount, currency, unparsedReference, bookingDate,
        otherPartyName, otherPartyBankCode, otherPartyAccountId, bookingText, valueDate,
        0, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, "", "", null, null, "", null)



    override fun toString(): String {
        return "${DateFormat.getDateInstance(DateFormat.MEDIUM).format(valueDate)} $amount $otherPartyName: $unparsedReference"
    }

}