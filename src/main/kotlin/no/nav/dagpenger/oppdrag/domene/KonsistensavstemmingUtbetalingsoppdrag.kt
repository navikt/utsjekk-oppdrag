package no.nav.dagpenger.oppdrag.domene

import java.time.LocalDateTime

data class KonsistensavstemmingUtbetalingsoppdrag(
    val fagsystem: String,
    val utbetalingsoppdrag: List<Utbetalingsoppdrag>,
    val avstemmingstidspunkt: LocalDateTime
)
