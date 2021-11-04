package net.codinux.banking.rest.domain.clients.hbci4j

import net.codinux.banking.rest.domain.clients.hbci4j.mapper.hbci4jModelMapper
import net.codinux.banking.rest.domain.clients.hbci4j.model.ConnectResult
import net.codinux.banking.rest.domain.clients.hbci4j.model.GetAccountTransactionsResult
import net.codinux.banking.rest.domain.model.*
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
import org.slf4j.LoggerFactory
import java.io.File
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*


class hbci4jBankingClient(
    private val bank: BankData,
    private val callback: BankingClientCallback,
    private val dataFolder: File = File("hbci4j")
) : IBankingClient {

    companion object {
        // the date format is hard coded in HBCIUtils.string2DateISO()
        val HbciLibDateFormat = SimpleDateFormat("yyyy-MM-dd")

        private val log = LoggerFactory.getLogger(hbci4jBankingClient::class.java)
    }


    private val mapper = hbci4jModelMapper()


    override fun getAccountInfo(): Response<BankData> {
        val connection = connect()
        closeConnection(connection)

        if(connection.successful) {
            connection.passport?.let { passport ->
                val accounts = passport.accounts
                if (accounts == null || accounts.size == 0) {
                    log.error("Could not get accounts for bank credentials {} {}: {}", bank.bankCode, bank.bankName, bank.loginName)
                    return Response("Could not get accounts for this bank credentials")
                }

                this.bank.accounts = mapper.mapAccounts(accounts, passport)

                return Response(bank)
            }
        }

        return Response(connection.error?.getInnerExceptionMessage() ?: "Could not connect")
    }


    override fun getTransactions(bank: BankData, param: GetAccountDataParameter): Response<RetrievedTransactionsWithAccount> {
        val connection = connect()
        val account = param.account

        connection.handle?.let { handle ->
            try {
                val result = executeJobsForGetAccountTransactions(handle, bank, param)
                val status = result.status

                // Pruefen, ob die Kommunikation mit der Bank grundsaetzlich geklappt hat
                if (!status.isOK) {
                    log.error("Could not connect to bank ${bank.bankCode} $status: ${status.errorString}")
                    return Response("Could not connect to bank ${bank.bankCode}: $status")
                }

                // Auswertung des Saldo-Abrufs.
                var balance: BigDecimal? = null
                if (param.alsoRetrieveBalance && result.balanceJob != null) {
                    val balanceResult = result.balanceJob.jobResult as GVRSaldoReq
                    if(balanceResult.isOK == false) {
                        log.error("Could not fetch balance of bank account $account: $balanceResult", balanceResult.getJobStatus().exceptions)
                        return Response("Could not fetch balance of bank account $account: $balanceResult")
                    }

                    balance = balanceResult.entries[0].ready.value.bigDecimalValue
                }


                // Das Ergebnis des Jobs koennen wir auf "GVRKUms" casten. Jobs des Typs "KUmsAll"
                // liefern immer diesen Typ.
                val transactionsResult = result.accountTransactionsJob.jobResult as GVRKUms

                // Pruefen, ob der Abruf der Umsaetze geklappt hat
                if (transactionsResult.isOK == false) {
                    log.error("Could not get fetch account transactions of bank account $account: $transactionsResult", transactionsResult.getJobStatus().exceptions)
                    return Response("Could not fetch account transactions of bank account $account: $transactionsResult")
                }

                return Response(mapper.mapGetTransactionsResult(result.account, transactionsResult, balance, param, connection.passport!!))
            }
            catch(e: Exception) {
                log.error("Could not get account transactions for bank ${bank.bankCode}", e)
                return Response(e.getInnerExceptionMessage())
            }
            finally {
                closeConnection(connection)
            }
        }

        closeConnection(connection)

        return Response(connection.error?.getInnerExceptionMessage() ?: "Could not connect")
    }

    private fun executeJobsForGetAccountTransactions(handle: HBCIHandler, bank: BankData, param: GetAccountDataParameter): GetAccountTransactionsResult {
        val konto = mapper.mapToKonto(bank, param.account)

        // 1. Auftrag fuer das Abrufen des Saldos erzeugen
        var balanceJob: HBCIJob? = null
        if (param.alsoRetrieveBalance) {
            val createdBalanceJob = handle.newJob("SaldoReq")
            createdBalanceJob.setParam("my", konto) // festlegen, welches Konto abgefragt werden soll.
            createdBalanceJob.addToQueue() // Zur Liste der auszufuehrenden Auftraege hinzufuegen

            balanceJob = createdBalanceJob
        }
        // 2. Auftrag fuer das Abrufen der Umsaetze erzeugen
        val accountTransactionsJob = handle.newJob("KUmsAll")
        accountTransactionsJob.setParam("my", konto) // festlegen, welches Konto abgefragt werden soll.
        // evtl. Datum setzen, ab welchem die Auszüge geholt werden sollen
        param.fromDate?.let {
            accountTransactionsJob.setParam("startdate", HbciLibDateFormat.format(it))
        }
        param.toDate?.let {
            accountTransactionsJob.setParam("enddate", HbciLibDateFormat.format(it))
        }
        accountTransactionsJob.addToQueue() // Zur Liste der auszufuehrenden Auftraege hinzufuegen

        // Hier koennen jetzt noch weitere Auftraege fuer diesen Bankzugang hinzugefuegt
        // werden. Z.Bsp. Ueberweisungen.

        // Alle Auftraege aus der Liste ausfuehren.
        val status = handle.execute()

        return GetAccountTransactionsResult(konto, balanceJob, accountTransactionsJob, status)
    }


    private fun connect(): ConnectResult {
        return connect(bank, HBCIVersion.HBCI_300)
    }

    private fun connect(bank: BankData, version: HBCIVersion): ConnectResult {
        // HBCI4Java initialisieren
        // In "props" koennen optional Kernel-Parameter abgelegt werden, die in der Klasse
        // org.kapott.hbci.manager.HBCIUtils (oben im Javadoc) beschrieben sind.
        val props = Properties()
        HBCIUtils.init(props, HbciCallback(bank, mapper, callback))

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
                bank.tanMethods = mapper.mapTanMethods(pinTanPassport)

                if (bank.selectedTanMethod != null) {
                    val currentTanMethodCode = pinTanPassport.getCurrentTANMethod(false)
                    bank.selectedTanMethod = bank.tanMethods.firstOrNull { it.bankInternalMethodCode == currentTanMethodCode }
                }


                mapper.mapTanMedia(pinTanPassport)?.let { bank.tanMedia = it }
                // TODO: how to get selectedTanMedium? This is not possible, isn't it?
            }
        }
    }

    private fun getPassportFile(bank: BankData): File {
        val hbciClientFolder = File(dataFolder, "hbci4j-client")
        hbciClientFolder.mkdirs()

        val passportFile = File(hbciClientFolder, "passport_${bank.bankCode}_${bank.loginName}.dat")

        passportFile.deleteOnExit() // ensure it gets cleaned up when JVM shuts down

        return passportFile
    }

    private fun closeConnection(connection: ConnectResult) {
        closeConnection(connection.handle, connection.passport)
    }

    private fun closeConnection(handle: HBCIHandler?, passport: HBCIPassport?) {
        // Sicherstellen, dass sowohl Passport als auch Handle nach Beendigung geschlossen werden.
        try {
            if (handle != null || passport != null) { // if both (or only passport?) is null then HBCIUtils.doneThread() throws an exception
                handle?.close()

                passport?.close()

                HBCIUtils.doneThread() // i hate static variables, here's one of the reasons why: Old callbacks and therefore credentials get stored in static variables and therefore always the first entered credentials have been used
            }
        } catch(e: Exception) { log.error("Could not close connection", e) }
    }

}