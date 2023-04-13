package dp.oppdrag.model

import java.time.LocalDateTime

data class KonsistensavstemmingRequest(
    val fagsystem: String,
    val perioderForBehandlinger: List<PerioderForBehandling>,
    val avstemmingstidspunkt: LocalDateTime
)