package dp.oppdrag.repository

import dp.oppdrag.model.*
import no.trygdeetaten.skjema.oppdrag.Mmel
import java.time.LocalDateTime

interface OppdragLagerRepository {

    fun hentOppdrag(oppdragId: OppdragId, versjon: Int = 0): OppdragLager
    fun hentUtbetalingsoppdrag(oppdragId: OppdragId, versjon: Int = 0): Utbetalingsoppdrag
    fun hentAlleVersjonerAvOppdrag(oppdragId: OppdragId): List<OppdragLager>
    fun opprettOppdrag(oppdragLager: OppdragLager, versjon: Int = 0)
    fun oppdaterStatus(oppdragId: OppdragId, oppdragLagerStatus: OppdragLagerStatus, versjon: Int = 0)
    fun oppdaterKvitteringsmelding(oppdragId: OppdragId, kvittering: Mmel, versjon: Int = 0)
    fun hentIverksettingerForGrensesnittavstemming(
        fomTidspunkt: LocalDateTime,
        tomTidspunkt: LocalDateTime,
        fagOmraade: String
    ): List<OppdragLager>

    fun hentUtbetalingsoppdragForKonsistensavstemming(
        fagsystem: String,
        behandlingIder: Set<String>
    ): List<UtbetalingsoppdragForKonsistensavstemming>
}
