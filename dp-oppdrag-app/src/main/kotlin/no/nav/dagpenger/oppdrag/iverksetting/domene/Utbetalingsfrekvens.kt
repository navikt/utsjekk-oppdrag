package no.nav.dagpenger.oppdrag.iverksetting.domene

@Suppress("unused")
enum class Utbetalingsfrekvens(val kode: String) {
    DAGLIG("DAG"),
    UKENTLIG("UKE"),
    MÅNEDLIG("MND"),
    DAGLIG_14("14DG"),
    ENGANGSUTBETALING("ENG"),
}
