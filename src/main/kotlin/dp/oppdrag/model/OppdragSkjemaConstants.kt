package dp.oppdrag.model

import no.trygdeetaten.skjema.oppdrag.TfradragTillegg
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar

class OppdragSkjemaConstants {

    companion object {
        val OPPDRAG_GJELDER_DATO_FOM: LocalDate = LocalDate.of(2000, 1, 1)
        const val KODE_AKSJON = "1"

        const val ENHET_TYPE = "BOS"
        const val ENHET = "8020"
        val ENHET_DATO_FOM: LocalDate = LocalDate.of(1900, 1, 1)

        val FRADRAG_TILLEGG = TfradragTillegg.T
        const val BRUK_KJOEREPLAN_DEFAULT = "N"
        const val BRUK_KJOEREPLAN_G_OMBEREGNING = "J"
    }
}

enum class OppdragStatus {
    LAGT_PAA_KOE,
    KVITTERT_OK,
    KVITTERT_MED_MANGLER,
    KVITTERT_FUNKSJONELL_FEIL,
    KVITTERT_TEKNISK_FEIL,
    KVITTERT_UKJENT;
}

enum class EndringsKode(val kode: String) {
    NY("NY"),
    UENDRET("UEND"),
    ENDRING("ENDR");

    companion object {
        fun fromKode(kode: String): EndringsKode {
            for (s in values()) {
                if (s.kode == kode) return s
            }
            throw IllegalArgumentException("Ingen Endringskode med kode=$kode")
        }
    }
}

enum class UtbetalingsfrekvensKode(val kode: String) {
    DAGLIG("DAG"),
    UKENTLIG("UKE"),
    MAANEDLIG("MND"),
    DAGLIG_14("14DG"),
    ENGANGSUTBETALING("ENG")
}

enum class SatsTypeKode(val kode: String) {
    DAGLIG("DAG"),
    UKENTLIG("UKE"),
    MAANEDLIG("MND"),
    DAGLIG_14("14DG"),
    ENGANGSBELOEP("ENG"),
    AARLIG("AAR"),
    A_KONTO("AKTO"),
    UKJENT("-");

    companion object {
        fun fromKode(kode: String): SatsTypeKode {
            for (s in values()) {
                if (s.kode == kode) return s
            }
            throw IllegalArgumentException("Ingen SatsTypeKode med kode=$kode")
        }
    }
}

enum class GradTypeKode(val kode: String) {
    UFOEREGRAD("UFOR"),
    UTBETALINGSGRAD("UBGR"),
    UTTAKSGRAD_ALDERSPENSJON("UTAP"),
    UTTAKSGRAD_AFP("AFPG")
}

fun LocalDate.toXMLDate(): XMLGregorianCalendar {
    return DatatypeFactory.newInstance()
        .newXMLGregorianCalendar(GregorianCalendar.from(atStartOfDay(ZoneId.systemDefault())))
}