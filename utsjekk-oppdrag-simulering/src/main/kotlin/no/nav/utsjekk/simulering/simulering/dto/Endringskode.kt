package no.nav.utsjekk.simulering.simulering.dto

import com.fasterxml.jackson.annotation.JsonCreator

enum class Endringskode(val verdi: String) {
    NY("NY"),
    ENDRING("ENDR"),
    ;

    companion object {
        @JvmStatic
        @JsonCreator
        fun deserialize(json: String) =
            try {
                Endringskode.valueOf(json)
            } catch (e: Exception) {
                throw RuntimeException(
                    "Ugyldig endringskode: $json. Forventet en av følgende: ${entries.toTypedArray().map { it.name }}",
                )
            }

        @JvmStatic
        @JsonCreator
        fun serialize(kode: Endringskode) = kode.verdi
    }
}
