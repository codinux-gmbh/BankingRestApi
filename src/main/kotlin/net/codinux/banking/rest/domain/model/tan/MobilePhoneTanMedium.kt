package net.codinux.banking.rest.domain.model.tan


open class MobilePhoneTanMedium(
    displayName: String,
    status: TanMediumStatus,
    val phoneNumber: String?

) : TanMedium(displayName, status) {

    override fun toString(): String {
        return "$displayName $status"
    }

}