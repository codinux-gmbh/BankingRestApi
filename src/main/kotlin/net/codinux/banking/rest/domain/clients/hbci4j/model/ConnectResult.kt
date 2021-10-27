package net.codinux.banking.rest.domain.clients.hbci4j.model

import org.kapott.hbci.manager.HBCIHandler
import org.kapott.hbci.passport.HBCIPassport


open class ConnectResult(
    val successful: Boolean,
    val error: Exception? = null,
    val handle: HBCIHandler? = null,
    val passport: HBCIPassport? = null
)