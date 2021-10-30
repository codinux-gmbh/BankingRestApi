package net.codinux.banking.rest.domain.model.tan

import com.fasterxml.jackson.annotation.JsonIgnore


class FlickerCode(
    val challengeHHD_UC: String,
    val parsedDataSet: String,
    val decodingError: Exception? = null
) {


    val decodingSuccessful: Boolean
        @JsonIgnore
        get() = decodingError == null


    override fun toString(): String {
        if (decodingSuccessful == false) {
            return "Decoding error: $decodingError"
        }

        return "Parsed $challengeHHD_UC to $parsedDataSet"
    }

}