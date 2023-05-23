package no.nav.dagpenger.oppdrag.util

import no.nav.dagpenger.kontrakter.utbetaling.Fagsystem
import no.nav.dagpenger.kontrakter.utbetaling.Opphør
import no.nav.dagpenger.kontrakter.utbetaling.Utbetalingsoppdrag
import no.nav.dagpenger.kontrakter.utbetaling.Utbetalingsperiode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

object TestUtbetalingsoppdrag {

    private val localDateTimeNow = LocalDateTime.now()
    private val localDateNow = LocalDate.now()

    fun utbetalingsoppdragMedTilfeldigAktoer() = Utbetalingsoppdrag(
        Utbetalingsoppdrag.KodeEndring.NY,
        Fagsystem.Dagpenger,
        UUID.randomUUID(),
        UUID.randomUUID().toString(), // Foreløpig plass til en 50-tegn string og ingen gyldighetssjekk
        "SAKSBEHANDLERID",
        localDateTimeNow,
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
                Utbetalingsperiode.SatsType.MND,
                "UTEBETALES_TIL",
                UUID.randomUUID()
            )
        )
    )
}
