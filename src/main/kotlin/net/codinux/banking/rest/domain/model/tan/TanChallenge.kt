package net.codinux.banking.rest.domain.model.tan

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes(
    JsonSubTypes.Type(TanChallenge::class, name = "EnterTan"),
    JsonSubTypes.Type(ImageTanChallenge::class, name = "Image"),
    JsonSubTypes.Type(FlickerCodeTanChallenge::class, name = "Flickercode")
)
open class TanChallenge protected constructor(
    val type: TanChallengeType,
    val messageToShowToUser: String,
    val tanMethod: TanMethod
    // TODO: add availableTanMethods, selectedTanMedium, availableTanMedia
) {

    constructor(messageToShowToUser: String, tanMethod: TanMethod) : this(TanChallengeType.EnterTan, messageToShowToUser, tanMethod)

    override fun toString(): String {
        return "$tanMethod: $messageToShowToUser"
    }

}