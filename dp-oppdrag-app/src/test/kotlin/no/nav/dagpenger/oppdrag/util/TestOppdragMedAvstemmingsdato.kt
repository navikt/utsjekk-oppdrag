package no.nav.dagpenger.oppdrag.util

import no.nav.dagpenger.kontrakter.felles.Fagsystem
import no.nav.dagpenger.kontrakter.felles.GeneriskId
import no.nav.dagpenger.kontrakter.felles.GeneriskIdSomUUID
import no.nav.dagpenger.kontrakter.felles.Satstype
import no.nav.dagpenger.kontrakter.oppdrag.Utbetalingsoppdrag
import no.nav.dagpenger.kontrakter.oppdrag.Utbetalingsperiode
import no.nav.dagpenger.kontrakter.oppdrag.Utbetalingstype
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal object TestOppdragMedAvstemmingsdato {
    private val FAGSAKID = UUID.randomUUID()
    private const val AKTØR = "12345678911"

    fun lagTestUtbetalingsoppdrag(
        avstemmingstidspunkt: LocalDateTime,
        fagsystem: Fagsystem = Fagsystem.DAGPENGER,
        stønadstype: String = "DPORAS",
        fagsak: GeneriskId = GeneriskIdSomUUID(FAGSAKID),
        vararg utbetalingsperiode: Utbetalingsperiode = arrayOf(lagUtbetalingsperiode(utbetalingstypeForKode(stønadstype))),
    ) = Utbetalingsoppdrag(
        kodeEndring = Utbetalingsoppdrag.KodeEndring.NY,
        fagsystem = fagsystem,
        saksnummer = fagsak,
        aktør = AKTØR,
        saksbehandlerId = "Z999999",
        avstemmingstidspunkt = avstemmingstidspunkt,
        utbetalingsperiode = utbetalingsperiode.toList(),
        iverksettingId = null,
    )

    private fun lagUtbetalingsperiode(
        utbetalingstype: Utbetalingstype = Utbetalingstype.DAGPENGER_ARBEIDSSØKER_ORDINÆR,
        periodeId: Long = 1,
        beløp: Int = 100,
        fom: LocalDate = LocalDate.now().withDayOfMonth(1),
        tom: LocalDate = LocalDate.now().plusYears(6),
    ) = Utbetalingsperiode(
        erEndringPåEksisterendePeriode = false,
        opphør = null,
        periodeId = periodeId,
        forrigePeriodeId = null,
        vedtaksdato = LocalDate.now(),
        klassifisering = utbetalingstype.kode,
        fom = fom,
        tom = tom,
        sats = beløp.toBigDecimal(),
        satstype = Satstype.MÅNEDLIG,
        utbetalesTil = AKTØR,
        behandlingId = GeneriskIdSomUUID(UUID.randomUUID()),
        utbetalingsgrad = 50,
    )

    private fun utbetalingstypeForKode(kode: String) =
        requireNotNull(
            Utbetalingstype.entries.find {
                it.kode == kode
            },
        )
}
