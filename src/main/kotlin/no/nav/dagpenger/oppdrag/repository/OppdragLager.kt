package no.nav.dagpenger.oppdrag.repository

import no.nav.dagpenger.kontrakter.utbetaling.OppdragId
import no.nav.dagpenger.kontrakter.utbetaling.Utbetalingsoppdrag
import no.nav.dagpenger.kontrakter.utbetaling.tilFagsystem
import no.nav.dagpenger.oppdrag.domene.OppdragStatus
import no.nav.dagpenger.oppdrag.domene.behandlingsIdForFørsteUtbetalingsperiode
import no.nav.dagpenger.oppdrag.iverksetting.Jaxb
import no.nav.dagpenger.oppdrag.iverksetting.OppdragMapper
import no.trygdeetaten.skjema.oppdrag.Mmel
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import java.time.LocalDateTime
import java.util.UUID

data class OppdragLager(
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
    val versjon: Int = 0
) {

    companion object {

        fun lagFraOppdrag(utbetalingsoppdrag: Utbetalingsoppdrag, oppdrag: Oppdrag, versjon: Int = 0): OppdragLager {
            return OppdragLager(
                personIdent = utbetalingsoppdrag.aktoer,
                fagsystem = utbetalingsoppdrag.fagSystem.kode,
                fagsakId = utbetalingsoppdrag.saksnummer.toString(),
                behandlingId = utbetalingsoppdrag.behandlingsIdForFørsteUtbetalingsperiode().toString(),
                avstemmingTidspunkt = utbetalingsoppdrag.avstemmingTidspunkt,
                utbetalingsoppdrag = utbetalingsoppdrag,
                utgåendeOppdrag = Jaxb.tilXml(oppdrag),
                kvitteringsmelding = null,
                versjon = versjon
            )
        }
    }
}

fun Utbetalingsoppdrag.somOppdragLagerMedVersjon(versjon: Int): OppdragLager {
    val tilOppdrag110 = OppdragMapper().tilOppdrag110(this)
    val oppdrag = OppdragMapper().tilOppdrag(tilOppdrag110)
    return OppdragLager.lagFraOppdrag(this, oppdrag, versjon)
}

val Utbetalingsoppdrag.somOppdragLager: OppdragLager
    get() {
        val tilOppdrag110 = OppdragMapper().tilOppdrag110(this)
        val oppdrag = OppdragMapper().tilOppdrag(tilOppdrag110)
        return OppdragLager.lagFraOppdrag(this, oppdrag)
    }

val OppdragLager.id: OppdragId
    get() {
        return OppdragId(this.fagsystem.tilFagsystem(), this.personIdent, UUID.fromString(this.behandlingId))
    }
