package no.nav.dagpenger.oppdrag.iverksetting.tilstand

import no.nav.dagpenger.kontrakter.felles.Fagsystem
import no.nav.dagpenger.kontrakter.felles.GeneriskId

data class OppdragId(
    val fagsystem: Fagsystem,
    // TODO bytt denne ut med fagsakId
    val personIdent: String,
    val behandlingId: GeneriskId,
    val iverksettingId: String? = null,
)
