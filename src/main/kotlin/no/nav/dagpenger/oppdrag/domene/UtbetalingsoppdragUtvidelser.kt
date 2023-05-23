package no.nav.dagpenger.oppdrag.domene

import no.nav.dagpenger.kontrakter.utbetaling.OppdragId
import no.nav.dagpenger.kontrakter.utbetaling.Utbetalingsoppdrag
import java.util.UUID

fun Utbetalingsoppdrag.behandlingsIdForFørsteUtbetalingsperiode(): UUID {

    return utbetalingsperiode[0].behandlingId
}

val Utbetalingsoppdrag.oppdragId
    get() = OppdragId(fagSystem, aktoer, behandlingsIdForFørsteUtbetalingsperiode())
