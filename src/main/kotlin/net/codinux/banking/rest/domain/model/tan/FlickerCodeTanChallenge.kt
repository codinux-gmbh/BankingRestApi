package net.codinux.banking.rest.domain.model.tan


class FlickerCodeTanChallenge(
    val flickerCode: FlickerCode,
    messageToShowToUser: String,
    tanMethod: TanMethod
) : TanChallenge(TanChallengeType.Flickercode, messageToShowToUser, tanMethod) {

    override fun toString(): String {
        return "$tanMethod $flickerCode: $messageToShowToUser"
    }

}