package no.nav.dagpenger.oppdrag.util

import no.nav.dagpenger.kontrakter.felles.Fagsystem
import no.nav.dagpenger.kontrakter.oppdrag.Utbetalingsoppdrag
import no.nav.dagpenger.kontrakter.oppdrag.Utbetalingsperiode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

object TestOppdragMedAvstemmingsdato {

    private val FAGSAKID = UUID.randomUUID()
    private val AKTOER = "12345678911"

    fun lagTestUtbetalingsoppdrag(
        avstemmingstidspunkt: LocalDateTime,
        fagsystem: Fagsystem = Fagsystem.Dagpenger,
        stønadstype: String = "DPORAS",
        fagsak: UUID = FAGSAKID,
        vararg utbetalingsperiode: Utbetalingsperiode = arrayOf(lagUtbetalingsperiode(stønadstype)),
    ) =
        Utbetalingsoppdrag(
            kodeEndring = Utbetalingsoppdrag.KodeEndring.NY,
            fagSystem = fagsystem,
            saksnummer = fagsak,
            aktoer = AKTOER,
            saksbehandlerId = "Z999999",
            avstemmingTidspunkt = avstemmingstidspunkt,
            utbetalingsperiode = utbetalingsperiode.toList()
        )

    fun lagUtbetalingsperiode(
        stønadstype: String = "DPORAS", // TODO Bytt med enum Stønadstype fra dp-kontrakter
        periodeId: Long = 1,
        beløp: Int = 100,
        fom: LocalDate = LocalDate.now().withDayOfMonth(1),
        tom: LocalDate = LocalDate.now().plusYears(6),
    ) =
        Utbetalingsperiode(
            erEndringPåEksisterendePeriode = false,
            opphør = null,
            periodeId = periodeId,
            forrigePeriodeId = null,
            datoForVedtak = LocalDate.now(),
            klassifisering = stønadstype,
            vedtakdatoFom = fom,
            vedtakdatoTom = tom,
            sats = beløp.toBigDecimal(),
            satsType = Utbetalingsperiode.SatsType.MND,
            utbetalesTil = AKTOER,
            behandlingId = UUID.randomUUID(),
            utbetalingsgrad = 50
        )
}
