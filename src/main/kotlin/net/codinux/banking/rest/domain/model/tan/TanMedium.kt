package net.codinux.banking.rest.domain.model.tan

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes(
    JsonSubTypes.Type(TanMedium::class, name = "Generic"),
    JsonSubTypes.Type(MobilePhoneTanMedium::class, name = "MobilePhone"),
    JsonSubTypes.Type(TanGeneratorTanMedium::class, name = "TanGenerator")
)
open class TanMedium(
    val type: TanMediumType,
    val displayName: String,
    val status: TanMediumStatus
) {

    constructor(displayName: String, status: TanMediumStatus) : this(TanMediumType.Generic, displayName, status)

    internal constructor() : this("", TanMediumStatus.Available) // for object deserializers


    override fun toString(): String {
        return "$displayName $status"
    }

}