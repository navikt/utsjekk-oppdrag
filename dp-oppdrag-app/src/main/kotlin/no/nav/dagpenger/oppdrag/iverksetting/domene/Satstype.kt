package no.nav.dagpenger.oppdrag.iverksetting.domene

import java.lang.IllegalArgumentException

enum class Satstype(val kode: String) {
    DAGLIG("DAG"),
    UKENTLIG("UKE"),
    MÅNEDLIG("MND"),
    DAGLIG_14("14DG"),
    ENGANGSBELØP("ENG"),
    ÅRLIG("AAR"),
    A_KONTO("AKTO"),
    UKJENT("-"),
    ;

    companion object {
        fun fromKode(kode: String) =
            requireNotNull(
                entries.find {
                    it.kode == kode
                },
            ) {
                throw IllegalArgumentException("Ingen SatsTypeKode med kode=$kode")
            }
    }
}
