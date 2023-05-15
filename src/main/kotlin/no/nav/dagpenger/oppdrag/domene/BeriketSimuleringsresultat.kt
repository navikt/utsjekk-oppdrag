package no.nav.dagpenger.oppdrag.domene

data class BeriketSimuleringsresultat(
    val detaljer: DetaljertSimuleringResultat,
    val oppsummering: Simuleringsoppsummering
)
