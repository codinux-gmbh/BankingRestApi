package net.codinux.banking.rest.domain.model

import net.codinux.banking.rest.domain.model.tan.EnterTanResult
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference


class EnterTanContext(
    val enterTanResult: AtomicReference<EnterTanResult>,
    val responseHolder: AsyncResponseHolder<*>,
    val countDownLatch: CountDownLatch,
    val tanRequestedTimeStamp: Date = Date()
)