package no.nav.dagpenger.oppdrag.iverksetting.tilstand

import no.nav.dagpenger.kontrakter.felles.Fagsystem
import no.nav.dagpenger.kontrakter.felles.GeneriskId
import no.nav.dagpenger.kontrakter.felles.GeneriskIdSomString
import no.nav.dagpenger.kontrakter.felles.GeneriskIdSomUUID
import no.nav.dagpenger.kontrakter.felles.tilFagsystem
import no.nav.dagpenger.oppdrag.iverksetting.domene.UuidKomprimator.dekomprimer
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import java.util.UUID

data class OppdragId(
    val fagsystem: Fagsystem,
    val fagsakId: GeneriskId,
    val behandlingId: GeneriskId,
    val iverksettingId: String?,
)

internal val Oppdrag.id: OppdragId
    get() =
        OppdragId(
            fagsystem = oppdrag110.kodeFagomraade.tilFagsystem(),
            fagsakId = oppdrag110.fagsystemId.tilGeneriskId(),
            behandlingId = oppdrag110.oppdragsLinje150?.get(0)?.henvisning!!.tilGeneriskId(),
            iverksettingId = null,
        )

internal val Oppdrag.dekomprimertId: OppdragId
    get() =
        OppdragId(
            fagsystem = oppdrag110.kodeFagomraade.tilFagsystem(),
            fagsakId = oppdrag110.fagsystemId.dekomprimer(),
            behandlingId = oppdrag110.oppdragsLinje150?.get(0)?.henvisning!!.dekomprimer(),
            iverksettingId = null,
        )

internal val OppdragLager.id: OppdragId
    get() =
        OppdragId(
            fagsystem = this.fagsystem.tilFagsystem(),
            fagsakId = this.fagsakId.tilGeneriskId(),
            behandlingId = this.behandlingId.tilGeneriskId(),
            iverksettingId = this.iverksettingId,
        )

internal fun String.tilGeneriskId(): GeneriskId =
    Result.runCatching { UUID.fromString(this@tilGeneriskId) }.fold(
        onSuccess = { GeneriskIdSomUUID(it) },
        onFailure = { GeneriskIdSomString(this) },
    )
