package no.nav.dagpenger.oppdrag.iverksetting.domene

import java.lang.IllegalArgumentException

internal enum class Endringskode(val kode: String) {
    NY("NY"),
    UENDRET("UEND"),
    ENDRING("ENDR"),
    ;

    companion object {
        fun fromKode(kode: String) =
            requireNotNull(
                entries.find {
                    it.kode == kode
                },
            ) {
                throw IllegalArgumentException("Ingen Endringskode med kode=$kode")
            }
    }
}
