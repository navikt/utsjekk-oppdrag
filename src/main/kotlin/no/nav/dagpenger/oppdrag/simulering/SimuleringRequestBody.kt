package no.nav.dagpenger.oppdrag.simulering

import com.fasterxml.jackson.annotation.JsonCreator
import no.nav.dagpenger.kontrakter.felles.Ident
import no.nav.dagpenger.kontrakter.felles.Personident
import java.time.LocalDate

data class SimuleringRequestBody(
    val fagområde: String,
    val fagsystemId: String,
    val fødselsnummer: Personident,
    val mottaker: Ident,
    val endringskode: Endringskode,
    val saksbehandler: String,
    val utbetalingsfrekvens: Utbetalingsfrekvens,
    val utbetalingslinjer: List<Utbetalingslinje>
)

data class Utbetalingslinje(
    val delytelseId: String,
    val endringskode: Endringskode,
    val klassekode: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val sats: Int,
    val grad: Int?,
    val refDelytelseId: String?,
    val refFagsystemId: String?,
    val datoStatusFom: LocalDate?,
    val statuskode: String?,
    val satstype: Satstype,
)

enum class Endringskode(val verdi: String) {
    NY("NY"),
    ENDRING("ENDR");

    companion object {
        @JvmStatic
        @JsonCreator
        fun deserialize(json: String) = Endringskode.valueOf(json)

        @JvmStatic
        @JsonCreator
        fun serialize(kode: Endringskode) = kode.verdi
    }
}

enum class Utbetalingsfrekvens(val verdi: String) {
    DAGLIG("DAG"),
    UKENTLIG("UKE"),
    HVER_FJORTENDE_DAG("14DG"),
    MÅNEDLIG("MND");

    companion object {
        @JvmStatic
        @JsonCreator
        fun deserialize(json: String) = Utbetalingsfrekvens.valueOf(json)

        @JvmStatic
        @JsonCreator
        fun serialize(frekvens: Utbetalingsfrekvens) = frekvens.verdi
    }
}

enum class Satstype(val verdi: String) {
    DAG("DAG"),
    MÅNED("MND");

    companion object {
        @JvmStatic
        @JsonCreator
        fun deserialize(json: String) = Satstype.valueOf(json)

        @JvmStatic
        @JsonCreator
        fun serialize(satstype: Satstype) = satstype.verdi
    }
}
