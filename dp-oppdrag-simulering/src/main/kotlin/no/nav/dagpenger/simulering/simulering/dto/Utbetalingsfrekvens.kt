package no.nav.dagpenger.simulering.simulering.dto

import com.fasterxml.jackson.annotation.JsonCreator
import jakarta.ws.rs.BadRequestException

enum class Utbetalingsfrekvens(val verdi: String) {
    DAGLIG("DAG"),
    UKENTLIG("UKE"),
    HVER_FJORTENDE_DAG("14DG"),
    MÅNEDLIG("MND"),
    ;

    companion object {
        @JvmStatic
        @JsonCreator
        fun deserialize(json: String) =
            try {
                Utbetalingsfrekvens.valueOf(json)
            } catch (e: Exception) {
                throw BadRequestException(
                    "Ugyldig utbetalingsfrekvens: $json. Forventet en av følgende: ${
                    entries.toTypedArray().map { it.name }
                    }",
                )
            }

        @JvmStatic
        @JsonCreator
        fun serialize(frekvens: Utbetalingsfrekvens) = frekvens.verdi
    }
}
