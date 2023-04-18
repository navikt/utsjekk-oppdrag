package dp.oppdrag.model

import java.time.LocalDateTime
import java.util.*

data class MellomlagringKonsistensavstemming(
    val id: UUID = UUID.randomUUID(),
    val fagsystem: String,
    val transaksjonsId: UUID,
    val antallOppdrag: Int,
    val totalBeloep: Long,
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
)
