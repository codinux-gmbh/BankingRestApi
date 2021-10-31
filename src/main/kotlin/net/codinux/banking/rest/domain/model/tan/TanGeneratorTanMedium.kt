package net.codinux.banking.rest.domain.model.tan


open class TanGeneratorTanMedium(
    displayName: String,
    status: TanMediumStatus,
    val cardNumber: String

) : TanMedium(TanMediumType.TanGenerator, displayName, status) {

    override fun toString(): String {
        return "$displayName $status"
    }

}