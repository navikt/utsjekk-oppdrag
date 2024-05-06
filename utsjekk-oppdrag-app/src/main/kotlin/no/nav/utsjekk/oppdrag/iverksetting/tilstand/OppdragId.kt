package no.nav.utsjekk.oppdrag.iverksetting.tilstand

import no.nav.utsjekk.kontrakter.felles.Fagsystem
import no.nav.utsjekk.kontrakter.felles.tilFagsystem
import no.trygdeetaten.skjema.oppdrag.Oppdrag

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
