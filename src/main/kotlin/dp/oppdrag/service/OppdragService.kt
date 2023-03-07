package dp.oppdrag.service

import dp.oppdrag.model.OppdragId
import dp.oppdrag.model.OppdragLager
import dp.oppdrag.model.Utbetalingsoppdrag
import no.trygdeetaten.skjema.oppdrag.Oppdrag

interface OppdragService {
    fun opprettOppdrag(utbetalingsoppdrag: Utbetalingsoppdrag, oppdrag: Oppdrag, versjon: Int)
    fun hentStatusForOppdrag(oppdragId: OppdragId): OppdragLager
}
