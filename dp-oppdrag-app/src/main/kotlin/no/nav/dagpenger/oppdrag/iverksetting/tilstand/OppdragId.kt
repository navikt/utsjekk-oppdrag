package no.nav.dagpenger.oppdrag.iverksetting.tilstand

import no.nav.dagpenger.kontrakter.felles.Fagsystem
import no.nav.dagpenger.kontrakter.felles.GeneriskId
import no.nav.dagpenger.kontrakter.felles.GeneriskIdSomString
import no.nav.dagpenger.kontrakter.felles.GeneriskIdSomUUID
import no.nav.dagpenger.kontrakter.felles.tilFagsystem
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import java.util.UUID

data class OppdragId(
    val fagsystem: Fagsystem,
    val fagsakId: String,
    val behandlingId: String,
    val iverksettingId: String?,
)

internal val Oppdrag.id: OppdragId
    get() =
        OppdragId(
            fagsystem = oppdrag110.kodeFagomraade.tilFagsystem(),
            fagsakId = oppdrag110.fagsystemId,
            behandlingId = oppdrag110.oppdragsLinje150?.get(0)?.henvisning!!,
            iverksettingId = null,
        )

internal val OppdragLager.id: OppdragId
    get() =
        OppdragId(
            fagsystem = this.fagsystem.tilFagsystem(),
            fagsakId = this.fagsakId,
            behandlingId = this.behandlingId,
            iverksettingId = this.iverksettingId,
        )

internal fun String.tilGeneriskId(): GeneriskId =
    Result.runCatching { UUID.fromString(this@tilGeneriskId) }.fold(
        onSuccess = { GeneriskIdSomUUID(it) },
        onFailure = { GeneriskIdSomString(this) },
    )
