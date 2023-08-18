package no.nav.dagpenger.oppdrag.util

import no.nav.dagpenger.kontrakter.felles.Fagsystem
import no.nav.dagpenger.kontrakter.oppdrag.Opphør
import no.nav.dagpenger.kontrakter.oppdrag.Utbetalingsoppdrag
import no.nav.dagpenger.kontrakter.oppdrag.Utbetalingsperiode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

object TestUtbetalingsoppdrag {

    private val localDateTimeNow = LocalDateTime.now()
    private val localDateNow = LocalDate.now()

    fun utbetalingsoppdragMedTilfeldigAktoer() = Utbetalingsoppdrag(
        kodeEndring = Utbetalingsoppdrag.KodeEndring.NY,
        fagSystem = Fagsystem.Dagpenger,
        saksnummer = UUID.randomUUID(),
        aktoer = UUID.randomUUID().toString(), // Foreløpig plass til en 50-tegn string og ingen gyldighetssjekk
        saksbehandlerId = "SAKSBEHANDLERID",
        utbetalingsperiode = listOf(
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
