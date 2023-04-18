package dp.oppdrag.model

import java.time.LocalDateTime

data class GrensesnittavstemmingRequest(
    val fra: LocalDateTime,
    val til: LocalDateTime
)
