package net.codinux.banking.rest.domain.model


open class BankCredentials(
    open val bankCode: String,
    open val loginName: String,
    open val password: String,
    open val finTsServerAddress: String? = null
) {

    internal constructor() : this("", "", "") // for object deserializers

}