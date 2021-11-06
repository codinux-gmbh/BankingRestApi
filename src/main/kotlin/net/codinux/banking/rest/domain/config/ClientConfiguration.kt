package net.codinux.banking.rest.domain.config

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


fun CountDownLatch.waitAtMaximumTillRequestTimeout() {
  this.await(ClientConfiguration.MaxWaitTimeForResponseMinutes, TimeUnit.MINUTES)
}


class ClientConfiguration {

  companion object {

    /**
     * Most TANs time out after 5 - 15 minutes. So waiting at maximum for more than 15 minutes should be a good value.
     */
    const val MaxWaitTimeForResponseMinutes = 20L

  }

}