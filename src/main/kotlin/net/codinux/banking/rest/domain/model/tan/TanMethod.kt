package net.codinux.banking.rest.domain.model.tan


class TanMethod(
    val displayName: String,
    val type: TanMethodType,
    val bankInternalMethodCode: String,
    val maxTanInputLength: Int? = null,
    val allowedTanFormat: AllowedTanFormat = AllowedTanFormat.Alphanumeric
) {


    internal constructor() : this("", TanMethodType.EnterTan, "") // for object deserializers


    override fun toString(): String {
        return "$displayName ($type, ${bankInternalMethodCode})"
    }

}