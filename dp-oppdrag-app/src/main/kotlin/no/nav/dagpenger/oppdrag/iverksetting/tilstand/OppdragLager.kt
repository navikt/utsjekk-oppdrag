package no.nav.dagpenger.oppdrag.iverksetting.tilstand

import no.nav.dagpenger.kontrakter.oppdrag.Utbetalingsoppdrag
import no.nav.dagpenger.oppdrag.iverksetting.domene.OppdragStatus
import no.nav.dagpenger.oppdrag.iverksetting.domene.fagsystemId
import no.nav.dagpenger.oppdrag.iverksetting.mq.OppdragXmlMapper
import no.trygdeetaten.skjema.oppdrag.Mmel
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import java.time.LocalDateTime
import java.util.UUID

internal data class OppdragLager(
    @Id
    @Column("id") val uuid: UUID = UUID.randomUUID(),
    val fagsystem: String,
    @Column("person_ident") val personIdent: String,
    @Column("fagsak_id") val fagsakId: String,
    @Column("behandling_id") val behandlingId: String,
    val utbetalingsoppdrag: Utbetalingsoppdrag,
    @Column("utgaaende_oppdrag") val utgåendeOppdrag: String,
    var status: OppdragStatus = OppdragStatus.LAGT_PAA_KOE,
    @Column("avstemming_tidspunkt") val avstemmingTidspunkt: LocalDateTime,
    @Column("opprettet_tidspunkt") val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
    val kvitteringsmelding: Mmel?,
    val versjon: Int = 0,
) {
    companion object {
        fun lagFraOppdrag(
            utbetalingsoppdrag: Utbetalingsoppdrag,
            oppdrag: Oppdrag,
            versjon: Int = 0,
        ) = OppdragLager(
            personIdent = utbetalingsoppdrag.aktoer,
            fagsystem = utbetalingsoppdrag.fagSystem.kode,
            fagsakId = utbetalingsoppdrag.fagsystemId,
            behandlingId = utbetalingsoppdrag.utbetalingsperiode.first().behandlingId.toString(),
            avstemmingTidspunkt = utbetalingsoppdrag.avstemmingTidspunkt,
            utbetalingsoppdrag = utbetalingsoppdrag,
            utgåendeOppdrag = OppdragXmlMapper.tilXml(oppdrag),
            kvitteringsmelding = null,
            versjon = versjon,
        )
    }
}
