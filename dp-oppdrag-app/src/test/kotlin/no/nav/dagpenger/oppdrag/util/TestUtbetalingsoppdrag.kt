package no.nav.dagpenger.oppdrag.util

import no.nav.dagpenger.kontrakter.felles.Fagsystem
import no.nav.dagpenger.kontrakter.felles.GeneriskIdSomUUID
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

    fun utbetalingsoppdragMedTilfeldigAktoer() =
        Utbetalingsoppdrag(
            kodeEndring = Utbetalingsoppdrag.KodeEndring.NY,
            fagsystem = Fagsystem.DAGPENGER,
            saksnummer = GeneriskIdSomUUID(UUID.randomUUID()),
            aktør = UUID.randomUUID().toString(),
            saksbehandlerId = "SAKSBEHANDLERID",
            utbetalingsperiode =
            listOf(
                Utbetalingsperiode(
                    false,
                    Opphør(localDateNow),
                    1,
                    null,
                    localDateNow,
                    "KLASSE A",
                    localDateNow,
                    localDateNow,
                    BigDecimal.ONE,
                    Utbetalingsperiode.Satstype.MND,
                    "UTEBETALES_TIL",
                    GeneriskIdSomUUID(UUID.randomUUID()),
                ),
            ),
        )
}

internal val Utbetalingsoppdrag.somOppdragLager: OppdragLager
    get() {
        val tilOppdrag110 = OppdragMapper.tilOppdrag110(this)
        val oppdrag = OppdragMapper.tilOppdrag(tilOppdrag110)

        return OppdragLager.lagFraOppdrag(this, oppdrag)
    }
