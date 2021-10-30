package net.codinux.banking.rest.domain.model

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
        responseReceivedLatch.await()

        return response!!
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