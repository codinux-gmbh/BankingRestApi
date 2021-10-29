package net.codinux.banking.rest.domain.clients.hbci4j

import net.codinux.banking.rest.domain.clients.hbci4j.mapper.hbci4jModelMapper
import net.codinux.banking.rest.domain.clients.hbci4j.model.ConnectResult
import net.codinux.banking.rest.domain.model.BankData
import net.codinux.banking.rest.domain.model.IBankingClient
import net.codinux.banking.rest.domain.model.Response
import net.dankito.utils.multiplatform.getInnerExceptionMessage
import org.kapott.hbci.GV.HBCIJob
import org.kapott.hbci.GV_Result.GVRKUms
import org.kapott.hbci.GV_Result.GVRSaldoReq
import org.kapott.hbci.manager.HBCIHandler
import org.kapott.hbci.manager.HBCIUtils
import org.kapott.hbci.manager.HBCIVersion
import org.kapott.hbci.passport.AbstractHBCIPassport
import org.kapott.hbci.passport.AbstractPinTanPassport
import org.kapott.hbci.passport.HBCIPassport
import org.kapott.hbci.status.HBCIExecStatus
import org.slf4j.LoggerFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


open class hbci4jBankingClient(
    protected val bank: BankData,
    protected val dataFolder: File = File("hbci4j")
) : IBankingClient {

    companion object {
        private val log = LoggerFactory.getLogger(hbci4jBankingClient::class.java)
    }


    protected val mapper = hbci4jModelMapper()


    override fun getAccountData(): Response<BankData> {
        val connection = connect()
        closeConnection(connection)

        if(connection.successful) {
            connection.passport?.let { passport ->
                val accounts = passport.accounts
                if (accounts == null || accounts.size == 0) {
                    log.error("Keine Konten ermittelbar")
                    return Response("Keine Konten ermittelbar") // TODO: translate
                }

                this.bank.accounts = mapper.mapAccounts(bank, accounts, passport)

                return Response(bank)
            }
        }

        return Response(connection.error?.getInnerExceptionMessage() ?: "Could not connect")
    }


    protected open fun connect(): ConnectResult {
        return connect(bank, HBCIVersion.HBCI_300)
    }

    protected open fun connect(bank: BankData, version: HBCIVersion): ConnectResult {
        // HBCI4Java initialisieren
        // In "props" koennen optional Kernel-Parameter abgelegt werden, die in der Klasse
        // org.kapott.hbci.manager.HBCIUtils (oben im Javadoc) beschrieben sind.
        val props = Properties()
        HBCIUtils.init(props, HbciCallback(bank, mapper))

        // In der Passport-Datei speichert HBCI4Java die Daten des Bankzugangs (Bankparameterdaten, Benutzer-Parameter, etc.).
        // Die Datei kann problemlos geloescht werden. Sie wird beim naechsten mal automatisch neu erzeugt,
        // wenn der Parameter "client.passport.PinTan.init" den Wert "1" hat (siehe unten).
        // Wir speichern die Datei der Einfachheit halber im aktuellen Verzeichnis.
        val passportFile = getPassportFile(bank)

        // Wir setzen die Kernel-Parameter zur Laufzeit. Wir koennten sie alternativ
        // auch oben in "props" setzen.
        HBCIUtils.setParam("client.passport.default", "PinTan") // Legt als Verfahren PIN/TAN fest.
        HBCIUtils.setParam("client.passport.PinTan.filename", passportFile.absolutePath)
        HBCIUtils.setParam("client.passport.PinTan.init", "1")

        var handle: HBCIHandler? = null
        var passport: HBCIPassport? = null

        try {
            // Erzeugen des Passport-Objektes.
            passport = AbstractHBCIPassport.getInstance()

            // Konfigurieren des Passport-Objektes.
            // Das kann alternativ auch alles ueber den Callback unten geschehen

            // Das Land.
            passport.country = "DE"

            // Server-Adresse angeben. Koennen wir entweder manuell eintragen oder direkt von HBCI4Java ermitteln lassen
            val info = HBCIUtils.getBankInfo(bank.bankCode)
            passport.host = info.pinTanAddress

            // TCP-Port des Servers. Bei PIN/TAN immer 443, da das ja ueber HTTPS laeuft.
            passport.port = 443

            // Art der Nachrichten-Codierung. Bei Chipkarte/Schluesseldatei wird
            // "None" verwendet. Bei PIN/TAN kommt "Base64" zum Einsatz.
            passport.filterType = "Base64"

            mapBankDataFromPassport(passport, bank)

            // Verbindung zum Server aufbauen
            handle = HBCIHandler(version.getId(), passport)


        }
        catch(e: Exception) {
            log.error("Could not connect to bank ${bank.bankCode}", e)
            closeConnection(handle, passport)

            return ConnectResult(false, e)
        }

        return ConnectResult(true, null, handle, passport)
    }

    private fun mapBankDataFromPassport(passport: HBCIPassport, bank: BankData) {
        // when passport has been created before, allowedTwostepMechanisms is already set (and HbciCallback's selectTanMethod() will not be called therefore we can't do it there)
        (passport as? AbstractPinTanPassport)?.let { pinTanPassport ->
            if (pinTanPassport.allowedTwostepMechanisms.isNotEmpty()) {
                bank.supportedTanMethods = mapper.mapTanMethods(pinTanPassport)

                if (bank.selectedTanMethod != null) {
                    val currentTanMethodCode = pinTanPassport.getCurrentTANMethod(false)
                    bank.selectedTanMethod = bank.supportedTanMethods.firstOrNull { it.bankInternalMethodCode == currentTanMethodCode }
                }

                // TODO: get TAN media
            }
        }
    }

    protected open fun getPassportFile(bank: BankData): File {
        val hbciClientFolder = File(dataFolder, "hbci4j-client")
        hbciClientFolder.mkdirs()

        val passportFile = File(hbciClientFolder, "passport_${bank.bankCode}_${bank.loginName}.dat")

        passportFile.deleteOnExit() // ensure it gets cleaned up when JVM shuts down

        return passportFile
    }

    protected open fun closeConnection(connection: ConnectResult) {
        closeConnection(connection.handle, connection.passport)
    }

    protected open fun closeConnection(handle: HBCIHandler?, passport: HBCIPassport?) {
        // Sicherstellen, dass sowohl Passport als auch Handle nach Beendigung geschlossen werden.
        try {
            handle?.close()

            passport?.close()

            HBCIUtils.doneThread() // i hate static variables, here's one of the reasons why: Old callbacks and therefore credentials get stored in static variables and therefor always the first entered credentials have been used
        } catch(e: Exception) { log.error("Could not close connection", e) }
    }

}