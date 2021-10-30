package net.codinux.banking.rest.domain.model.tan


class ImageTanChallenge(
    val image: TanImage,
    messageToShowToUser: String,
    tanMethod: TanMethod

    ) : TanChallenge(TanChallengeType.Image, messageToShowToUser, tanMethod) {

    override fun toString(): String {
        return "$tanMethod $image: $messageToShowToUser"
    }

}