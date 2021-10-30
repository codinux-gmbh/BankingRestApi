package net.codinux.banking.rest.domain.model.tan


class TanRequired(
    val tanRequestId: String,
    val tanChallenge: TanChallenge
)