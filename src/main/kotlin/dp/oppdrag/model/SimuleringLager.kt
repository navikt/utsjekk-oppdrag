package dp.oppdrag.model

import dp.oppdrag.defaultObjectMapper
import dp.oppdrag.defaultXmlMapper
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningRequest
import java.time.LocalDateTime
import java.util.*

data class SimuleringLager(
    val id: UUID = UUID.randomUUID(),
    val fagsystem: String,
    val fagsakId: String,
    val behandlingId: String,
    val utbetalingsoppdrag: String,
    val requestXml: String,
    var responseXml: String? = null,
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now()
) {

    companion object {
        fun lagFraOppdrag(
            utbetalingsoppdrag: Utbetalingsoppdrag,
            request: SimulerBeregningRequest
        ): SimuleringLager {
            return SimuleringLager(
                fagsystem = utbetalingsoppdrag.fagSystem,
                fagsakId = utbetalingsoppdrag.saksnummer,
                behandlingId = utbetalingsoppdrag.behandlingsIdForFoersteUtbetalingsperiode(),
                utbetalingsoppdrag = defaultObjectMapper.writeValueAsString(utbetalingsoppdrag),
                requestXml = defaultXmlMapper.writeValueAsString(request)
            )
        }
    }
}
