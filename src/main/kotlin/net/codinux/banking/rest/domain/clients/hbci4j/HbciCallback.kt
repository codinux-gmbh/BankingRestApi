package net.codinux.banking.rest.domain.clients.hbci4j

import net.codinux.banking.rest.domain.clients.hbci4j.mapper.hbci4jModelMapper
import net.codinux.banking.rest.domain.model.BankData
import net.codinux.banking.rest.domain.model.BankingClientCallback
import net.codinux.banking.rest.domain.model.tan.*
import org.kapott.hbci.callback.AbstractHBCICallback
import org.kapott.hbci.callback.HBCICallback
import org.kapott.hbci.manager.HBCIUtils
import org.kapott.hbci.manager.MatrixCode
import org.kapott.hbci.manager.QRCode
import org.kapott.hbci.passport.AbstractPinTanPassport
import org.kapott.hbci.passport.HBCIPassport
import org.slf4j.LoggerFactory


/**
 * Ueber diesen Callback kommuniziert HBCI4Java mit dem Benutzer und fragt die benoetigten
 * Informationen wie Benutzerkennung, PIN usw. ab.
 */
class HbciCallback(
    private val bank: BankData,
    private val mapper: hbci4jModelMapper,
    private val callback: BankingClientCallback
) : AbstractHBCICallback() {

    companion object {
        private val log = LoggerFactory.getLogger(HbciCallback::class.java)
    }


    override fun callback(passport: HBCIPassport, reason: Int, msg: String, datatype: Int, retData: StringBuffer) {
        log.debug("Callback: [$reason] $msg ($retData)")

        // Diese Funktion ist wichtig. Ueber die fragt HBCI4Java die benoetigten Daten von uns ab.
        when (reason) {
            // Mit dem Passwort verschluesselt HBCI4Java die Passport-Datei.
            // Wir nehmen hier der Einfachheit halber direkt die PIN. In der Praxis
            // sollte hier aber ein staerkeres Passwort genutzt werden.
            // Die Ergebnis-Daten muessen in dem StringBuffer "retData" platziert werden.
            // if you like or need to change your pin, return your old one for NEED_PASSPHRASE_LOAD and your new
            // one for NEED_PASSPHRASE_SAVE
            HBCICallback.NEED_PASSPHRASE_LOAD, HBCICallback.NEED_PASSPHRASE_SAVE -> retData.replace(0, retData.length, bank.password)


            /*      Customer (authentication) data           */

            // BLZ wird benoetigt
            HBCICallback.NEED_BLZ -> retData.replace(0, retData.length, bank.bankCode)

            // Die Benutzerkennung
            HBCICallback.NEED_USERID -> retData.replace(0, retData.length, bank.loginName)

            // Die Kundenkennung. Meist identisch mit der Benutzerkennung.
            // Bei manchen Banken kann man die auch leer lassen
            HBCICallback.NEED_CUSTOMERID -> retData.replace(0, retData.length, bank.loginName)

            // PIN wird benoetigt
            HBCICallback.NEED_PT_PIN -> retData.replace(0, retData.length, bank.password)


            /*          TAN         */

            // ADDED: Auswaehlen welches PinTan Verfahren verwendet werden soll
            HBCICallback.NEED_PT_SECMECH -> selectTanMethod(passport, retData)

            // chipTan or simple TAN request (iTAN, smsTAN, ...)
            HBCICallback.NEED_PT_TAN -> getTanFromUser(msg, retData)

            // chipTAN-QR
            HBCICallback.NEED_PT_QRTAN -> { // use class QRCode to display QR code
                val qrCode = QRCode(retData.toString(), msg)
                getTanFromUser(TanImage(qrCode.mimetype, qrCode.image), msg, retData)
            }

            // photoTan
            HBCICallback.NEED_PT_PHOTOTAN -> { // use class MatrixCode to display photo
                val matrixCode = MatrixCode(retData.toString())
                getTanFromUser(TanImage(matrixCode.mimetype, matrixCode.image), msg, retData)
            }

            // select which TAN medium to use
            HBCICallback.NEED_PT_TANMEDIA -> {
                selectTanMedium(msg, retData)
            }

            // wrong pin entered -> inform user
            HBCICallback.WRONG_PIN -> {
                log.info("TODO: user entered wrong pin: $msg ($retData)")
            }

            // UserId changed -> inform user
            HBCICallback.USERID_CHANGED -> { // im Parameter retData stehen die neuen Daten im Format UserID|CustomerID drin
                log.info("TODO: UserId changed: $msg ($retData)")
            }

            // user entered wrong Bankleitzahl or Kontonummer -> inform user
            HBCICallback.HAVE_CRC_ERROR -> { // retData contains wrong values in form "BLZ|KONTONUMMER". Set correct ones in the same form in retData
                log.info("TODO: wrong Bankleitzahl or Kontonummer entered: $msg ($retData)")
            }

            // user entered wrong IBAN -> inform user
            HBCICallback.HAVE_IBAN_ERROR -> { // retData contains wrong IBAN. Set correct IBAN in retData
                log.info("TODO: wrong IBAN entered: $msg ($retData)")
            }

            // message from bank to user. should get displayed to user
            HBCICallback.HAVE_INST_MSG -> {
                // TODO: inform user
                log.error("TODO: inform user, received a message from bank: $msg\n$retData")
            }

            // Manche Fehlermeldungen werden hier ausgegeben
            HBCICallback.HAVE_ERROR -> { // to ignore error set an empty String in retData
                // TODO: inform user
                log.error("TODO: inform user, error occurred: $msg\n$retData")
            }

            else -> { // Wir brauchen nicht alle der Callbacks
            }
        }
    }


    private fun getTanFromUser(messageToShowToUser: String, returnData: StringBuffer) {
        // Wenn per "retData" Daten uebergeben wurden, dann enthalten diese
        // den fuer chipTAN optisch zu verwendenden Flickercode.
        // Falls nicht, ist es eine TAN-Abfrage, fuer die keine weiteren
        // Parameter benoetigt werden (z.B. smsTAN, iTAN oder aehnliches)

        // Die Variable "msg" aus der Methoden-Signatur enthaelt uebrigens
        // den bankspezifischen Text mit den Instruktionen fuer den User.
        // Der Text aus "msg" sollte daher im Dialog dem User angezeigt
        // werden.

        val challengeHHD_UC = returnData.toString()

        val tanChallenge = if (challengeHHD_UC.isBlank()) {
            TanChallenge(messageToShowToUser, bank.selectedTanMethod!!)
        }
        else {
            // for Sparkasse messageToShowToUser started with "chipTAN optisch\nTAN-Nummer\n\n"
            val usefulMessage = messageToShowToUser.split("\n").last().trim()

            FlickerCodeTanChallenge(FlickerCode("", challengeHHD_UC), usefulMessage, bank.selectedTanMethod!!)
        }

        getTanFromUser(tanChallenge, returnData)
    }

    private fun getTanFromUser(tanImage: TanImage, messageToShowToUser: String, returnData: StringBuffer) {
        val tanChallenge = ImageTanChallenge(tanImage, messageToShowToUser, bank.selectedTanMethod!!)

        getTanFromUser(tanChallenge, returnData)
    }

    private fun getTanFromUser(tanChallenge: TanChallenge, returnData: StringBuffer) {
        callback.enterTan(bank, tanChallenge) { result ->
            result.enteredTan?.let { enteredTan ->
                returnData.replace(0, returnData.length, enteredTan)
            }
        }
    }


    private fun selectTanMethod(passport: HBCIPassport, returnData: StringBuffer) {
        if (/* bank.supportedTanMethods.isEmpty() && */ passport is AbstractPinTanPassport) {
            val supportedTanMethodsString = returnData.toString()
            bank.supportedTanMethods = mapper.mapTanMethods(passport, supportedTanMethodsString)
        }

        val supportedTanMethods = bank.supportedTanMethods

        if (supportedTanMethods.isNotEmpty()) {
            // select any method, user then can select her preferred one in EnterTanDialog; try not to select 'chipTAN manuell'
            bank.selectedTanMethod = supportedTanMethods.firstOrNull { it.type == TanMethodType.AppTan }
                ?: supportedTanMethods.firstOrNull { it.type == TanMethodType.SmsTan }
                ?: supportedTanMethods.firstOrNull { it.displayName.contains("manuell", true) }
                ?: supportedTanMethods.firstOrNull()

            returnData.replace(0, returnData.length, bank.selectedTanMethod?.bankInternalMethodCode)
        }
    }

    private fun selectTanMedium(message: String, returnData: StringBuffer) {
        log.info("Select TAN medium: $message ($returnData)")

        mapper.mapTanMedia(returnData.toString())?.let { tanMedia ->
            bank.tanMedia = tanMedia

            if (tanMedia.isNotEmpty()) {
                returnData.replace(0, returnData.length, tanMedia[0].displayName)
            }
        }
    }


    override fun log(msg: String?, level: Int, date: java.util.Date?, trace: StackTraceElement?) {
        when (level) {
            HBCIUtils.LOG_ERR -> log.error(msg)
            HBCIUtils.LOG_WARN -> log.warn(msg)
            HBCIUtils.LOG_INFO-> log.info(msg)
            HBCIUtils.LOG_DEBUG, HBCIUtils.LOG_DEBUG2 -> log.debug(msg)
            else -> log.trace(msg)
        }
    }

    override fun status(passport: HBCIPassport, statusTag: Int, o: Array<Any>?) {
        // So aehnlich wie log(String,int,Date,StackTraceElement) jedoch fuer Status-Meldungen.
        val param = o?.joinToString() ?: ""

        when (statusTag) {
            HBCICallback.STATUS_MSG_RAW_SEND -> log.debug("Sending message:\n$param")
            HBCICallback.STATUS_MSG_RAW_RECV -> log.debug("Received message:\n$param")
//            else -> log.debug("New status [$statusTag]: $param")
        }
    }

}