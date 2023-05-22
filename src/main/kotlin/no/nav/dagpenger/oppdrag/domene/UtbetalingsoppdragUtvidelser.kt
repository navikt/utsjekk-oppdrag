package no.nav.dagpenger.oppdrag.domene

import no.nav.dagpenger.kontrakter.utbetaling.Utbetalingsoppdrag

fun Utbetalingsoppdrag.behandlingsIdForFørsteUtbetalingsperiode(): String {

    return utbetalingsperiode[0].behandlingId
}

val Utbetalingsoppdrag.oppdragId
    get() = OppdragId(fagSystem, aktoer, behandlingsIdForFørsteUtbetalingsperiode())
