package no.nav.dagpenger.oppdrag.util

import no.nav.dagpenger.kontrakter.felles.Fagsystem
import no.nav.dagpenger.kontrakter.felles.GeneriskIdSomUUID
import no.nav.dagpenger.kontrakter.felles.Satstype
import no.nav.dagpenger.kontrakter.oppdrag.Opphør
import no.nav.dagpenger.kontrakter.oppdrag.Utbetalingsoppdrag
import no.nav.dagpenger.kontrakter.oppdrag.Utbetalingsperiode
import no.nav.dagpenger.oppdrag.iverksetting.domene.OppdragMapper
import no.nav.dagpenger.oppdrag.iverksetting.tilstand.OppdragLager
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

object TestUtbetalingsoppdrag {
    private val localDateNow = LocalDate.now()

    fun utbetalingsoppdragMedTilfeldigAktoer(iverksettingId: String? = null) =
        Utbetalingsoppdrag(
            kodeEndring = Utbetalingsoppdrag.KodeEndring.NY,
            fagsystem = Fagsystem.DAGPENGER,
            saksnummer = GeneriskIdSomUUID(UUID.randomUUID()),
            aktør = UUID.randomUUID().toString(),
            saksbehandlerId = "SAKSBEHANDLERID",
            utbetalingsperiode =
            listOf(
                Utbetalingsperiode(
                    erEndringPåEksisterendePeriode = false,
                    opphør = Opphør(localDateNow),
                    periodeId = 1,
                    forrigePeriodeId = null,
                    vedtaksdato = localDateNow,
                    klassifisering = "KLASSE A",
                    fom = localDateNow,
                    tom = localDateNow,
                    sats = BigDecimal.ONE,
                    satstype = Satstype.MÅNEDLIG,
                    utbetalesTil = "UTEBETALES_TIL",
                    behandlingId = GeneriskIdSomUUID(UUID.randomUUID()),
                ),
            ),
            iverksettingId = iverksettingId,
        )
}

internal val Utbetalingsoppdrag.somOppdragLager: OppdragLager
    get() {
        val tilOppdrag110 = OppdragMapper.tilOppdrag110(this)
        val oppdrag = OppdragMapper.tilOppdrag(tilOppdrag110)

        return OppdragLager.lagFraOppdrag(this, oppdrag)
    }
