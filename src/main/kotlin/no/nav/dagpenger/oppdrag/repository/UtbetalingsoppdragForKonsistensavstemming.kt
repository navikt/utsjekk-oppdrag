package no.nav.dagpenger.oppdrag.repository

import no.nav.dagpenger.oppdrag.domene.Utbetalingsoppdrag

data class UtbetalingsoppdragForKonsistensavstemming(
    val fagsakId: String,
    val behandlingId: String,
    val utbetalingsoppdrag: Utbetalingsoppdrag
)
