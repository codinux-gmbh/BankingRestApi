package net.codinux.banking.rest.domain.model

import net.codinux.banking.rest.domain.config.ClientConfiguration
import net.codinux.banking.rest.domain.config.waitAtMaximumTillRequestTimeout
import java.util.concurrent.CountDownLatch


class AsyncResponseHolder<T> {

    private var responseReceivedLatch = CountDownLatch(1)


    var response: Response<T>? = null
        private set


    fun setResponse(response: Response<T>) {
        this.response = response

        signalResponseReceived()
    }


    fun waitForResponse(): Response<T> {
        responseReceivedLatch.waitAtMaximumTillRequestTimeout()

        return response ?: Response("Could not get response after ${ClientConfiguration.MaxWaitTimeForResponseMinutes}", ErrorType.InternalError)
    }

    fun resetAfterEnteringTan() {
        this.response = null

        responseReceivedLatch = CountDownLatch(1)
    }

    private fun signalResponseReceived() {
        responseReceivedLatch.countDown()
    }


    override fun toString(): String {
        return "Error: ${response?.error}, TAN requested: ${response?.tanRequired}, success: ${response?.data}"
    }

}