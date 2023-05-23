package no.nav.dagpenger.oppdrag.domene

import no.nav.dagpenger.kontrakter.utbetaling.Fagsystem
import java.util.UUID

data class OppdragId(
    val fagsystem: Fagsystem,
    val personIdent: String,
    val behandlingsId: UUID
) {
    override fun toString(): String = "OppdragId(fagsystem=$fagsystem, behandlingsId=$behandlingsId)"
}
