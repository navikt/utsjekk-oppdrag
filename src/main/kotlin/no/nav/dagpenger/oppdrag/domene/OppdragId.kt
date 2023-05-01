package no.nav.dagpenger.oppdrag.domene

data class OppdragId(
    val fagsystem: String,
    val personIdent: String,
    val behandlingsId: String
) {
    override fun toString(): String = "OppdragId(fagsystem=$fagsystem, behandlingsId=$behandlingsId)"
}
