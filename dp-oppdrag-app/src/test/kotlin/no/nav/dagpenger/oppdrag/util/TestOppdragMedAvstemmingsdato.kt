package no.nav.dagpenger.oppdrag.util

import no.nav.dagpenger.kontrakter.felles.Fagsystem
import no.nav.dagpenger.kontrakter.oppdrag.UtbetalingType
import no.nav.dagpenger.kontrakter.oppdrag.Utbetalingsoppdrag
import no.nav.dagpenger.kontrakter.oppdrag.Utbetalingsperiode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal object TestOppdragMedAvstemmingsdato {
    private val FAGSAKID = UUID.randomUUID()
    private const val AKTØR = "12345678911"

    fun lagTestUtbetalingsoppdrag(
        avstemmingstidspunkt: LocalDateTime,
        fagsystem: Fagsystem = Fagsystem.Dagpenger,
        stønadstype: String = "DPORAS",
        fagsak: UUID = FAGSAKID,
        vararg utbetalingsperiode: Utbetalingsperiode = arrayOf(lagUtbetalingsperiode(utbetalingstypeForKode(stønadstype))),
    ) = Utbetalingsoppdrag(
        kodeEndring = Utbetalingsoppdrag.KodeEndring.NY,
        fagSystem = fagsystem,
        saksnummer = fagsak,
        aktoer = AKTØR,
        saksbehandlerId = "Z999999",
        avstemmingTidspunkt = avstemmingstidspunkt,
        utbetalingsperiode = utbetalingsperiode.toList(),
    )

    private fun lagUtbetalingsperiode(
        utbetalingstype: UtbetalingType = UtbetalingType.DAGPENGER_ARBEIDSSOKER_ORDINAER,
        periodeId: Long = 1,
        beløp: Int = 100,
        fom: LocalDate = LocalDate.now().withDayOfMonth(1),
        tom: LocalDate = LocalDate.now().plusYears(6),
    ) = Utbetalingsperiode(
        erEndringPåEksisterendePeriode = false,
        opphør = null,
        periodeId = periodeId,
        forrigePeriodeId = null,
        datoForVedtak = LocalDate.now(),
        klassifisering = utbetalingstype.kode,
        vedtakdatoFom = fom,
        vedtakdatoTom = tom,
        sats = beløp.toBigDecimal(),
        satsType = Utbetalingsperiode.SatsType.MND,
        utbetalesTil = AKTØR,
        behandlingId = UUID.randomUUID(),
        utbetalingsgrad = 50,
    )

    private fun utbetalingstypeForKode(kode: String) =
        requireNotNull(
            UtbetalingType.entries.find {
                it.kode == kode
            },
        )
}
