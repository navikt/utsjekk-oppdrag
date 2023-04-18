package dp.oppdrag.model

import no.trygdeetaten.skjema.oppdrag.Mmel
import java.time.LocalDateTime
import java.util.*

data class OppdragLager(
    val uuid: UUID = UUID.randomUUID(),
    val fagsystem: String,
    val personIdent: String,
    val fagsakId: String,
    val behandlingId: String,
    val utbetalingsoppdrag: Utbetalingsoppdrag,
    val utgaaendeOppdrag: String,
    var status: OppdragLagerStatus = OppdragLagerStatus.LAGT_PAA_KOE,
    val avstemmingTidspunkt: LocalDateTime,
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
    val kvitteringsmelding: Mmel?,
    val versjon: Int = 0
) {

    companion object {

        fun lagFraOppdrag(
            utbetalingsoppdrag: Utbetalingsoppdrag,
            utgaaendeOppdrag: String,
            versjon: Int = 0
        ): OppdragLager {
            return OppdragLager(
                personIdent = utbetalingsoppdrag.aktoer,
                fagsystem = utbetalingsoppdrag.fagSystem,
                fagsakId = utbetalingsoppdrag.saksnummer,
                behandlingId = utbetalingsoppdrag.behandlingsIdForFoersteUtbetalingsperiode(),
                avstemmingTidspunkt = utbetalingsoppdrag.avstemmingTidspunkt,
                utbetalingsoppdrag = utbetalingsoppdrag,
                utgaaendeOppdrag = utgaaendeOppdrag,
                kvitteringsmelding = null,
                versjon = versjon
            )
        }
    }
}
