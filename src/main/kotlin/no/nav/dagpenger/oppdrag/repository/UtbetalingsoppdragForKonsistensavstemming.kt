package no.nav.dagpenger.oppdrag.repository

import no.nav.dagpenger.kontrakter.utbetaling.Utbetalingsoppdrag

data class UtbetalingsoppdragForKonsistensavstemming(
    val fagsakId: String,
    val behandlingId: String,
    val utbetalingsoppdrag: Utbetalingsoppdrag
)
