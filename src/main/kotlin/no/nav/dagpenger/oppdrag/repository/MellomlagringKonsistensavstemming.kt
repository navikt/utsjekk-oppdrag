package no.nav.dagpenger.oppdrag.repository

import no.nav.dagpenger.kontrakter.utbetaling.Fagsystem
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.UUID

@Table("mellomlagring_konsistensavstemming")
data class MellomlagringKonsistensavstemming(
    @Id val id: UUID = UUID.randomUUID(),
    val fagsystem: Fagsystem,
    val transaksjonsId: UUID,
    val antallOppdrag: Int,
    @Column("total_belop") val totalBeløp: Long,
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
)
