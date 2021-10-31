package net.codinux.banking.rest.domain.clients.fints4k

import net.dankito.banking.fints.webclient.IWebClient
import net.dankito.banking.fints.webclient.WebClientResponse
import net.dankito.utils.web.client.OkHttpWebClient
import net.dankito.utils.web.client.RequestParameters
import org.slf4j.LoggerFactory


class SynchronousWebClient : IWebClient {

  companion object {
    private val log = LoggerFactory.getLogger(SynchronousWebClient::class.java)
  }


  private val client = OkHttpWebClient()


  override fun post(url: String, body: String, contentType: String, userAgent: String, callback: (WebClientResponse) -> Unit) {
    try {
      val response = client.post(RequestParameters(url, body, contentType, userAgent))

      callback(WebClientResponse(response.isSuccessful, response.responseCode, response.error, response.body))
    } catch (e: Exception) {
      log.error("Could not send request to url '$url'", e)

      callback(WebClientResponse(false, error = e))
    }
  }

}