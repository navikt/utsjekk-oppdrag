package dp.oppdrag.sender

import no.trygdeetaten.skjema.oppdrag.Oppdrag

interface OppdragSender {
    fun sendOppdrag(oppdrag: Oppdrag): String
}
