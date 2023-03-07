package dp.oppdrag.sender

import no.trygdeetaten.skjema.oppdrag.Oppdrag

class OppdragSenderMQ : OppdragSender {

    override fun sendOppdrag(oppdrag: Oppdrag): String {
        return "OK"
    }
}
