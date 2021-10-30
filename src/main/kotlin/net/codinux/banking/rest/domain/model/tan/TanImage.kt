package net.codinux.banking.rest.domain.model.tan

import com.fasterxml.jackson.annotation.JsonIgnore


class TanImage(
    val mimeType: String,
    val imageBytes: ByteArray,
    val decodingError: Exception? = null
) {

    val decodingSuccessful: Boolean
        @JsonIgnore
        get() = decodingError == null


    override fun toString(): String {
        if (decodingSuccessful == false) {
            return "Decoding error: $decodingError"
        }

        return "$mimeType ${imageBytes.size} bytes"
    }

}