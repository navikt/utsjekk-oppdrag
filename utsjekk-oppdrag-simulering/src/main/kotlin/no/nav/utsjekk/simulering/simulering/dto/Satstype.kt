package no.nav.utsjekk.simulering.simulering.dto

import com.fasterxml.jackson.annotation.JsonCreator
import jakarta.ws.rs.BadRequestException

enum class Satstype(val verdi: String) {
    DAG("DAG"),
    MÅNED("MND"),
    ;

    companion object {
        @JvmStatic
        @JsonCreator
        fun deserialize(json: String) =
            try {
                Satstype.valueOf(json)
            } catch (e: Exception) {
                throw BadRequestException(
                    "Ugyldig satstype: $json. Forventet en av følgende: ${
                    Utbetalingsfrekvens.entries.toTypedArray().map { it.name }
                    }",
                )
            }

        @JvmStatic
        @JsonCreator
        fun serialize(satstype: Satstype) = satstype.verdi
    }
}
