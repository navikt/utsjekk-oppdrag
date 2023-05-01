package no.nav.dagpenger.oppdrag.domene

import java.time.LocalDateTime

data class KonsistensavstemmingRequestV2(
    val fagsystem: String,
    val perioderForBehandlinger: List<PerioderForBehandling>,
    val avstemmingstidspunkt: LocalDateTime
)
