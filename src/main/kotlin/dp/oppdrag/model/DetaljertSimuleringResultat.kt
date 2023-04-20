package dp.oppdrag.model

import java.math.BigDecimal
import java.time.LocalDate

data class DetaljertSimuleringResultat(val simuleringMottaker: List<SimuleringMottaker>)

data class SimuleringMottaker(
    val simulertPostering: List<SimulertPostering>,
    val mottakerNummer: String?,
    val mottakerType: MottakerType
)

data class SimulertPostering(
    val fagOmraadeKode: FagOmraadeKode,
    val erFeilkonto: Boolean?,
    val fom: LocalDate,
    val tom: LocalDate,
    val betalingType: BetalingType,
    val beloep: BigDecimal,
    val posteringType: PosteringType,
    val forfallsdato: LocalDate,
    val utenInntrekk: Boolean
)

enum class FagOmraadeKode {
    DP; // TODO: Slette?

    companion object {
        fun fraKode(kode: String): FagOmraadeKode {
            FagOmraadeKode.values().forEach {
                if (it.name == kode) return it
            }
            throw IllegalArgumentException("No enum constant with name=$kode")
        }
    }
}

enum class BetalingType {
    DEBIT,
    KREDIT;
}

enum class PosteringType {
    YTELSE,
    FEILUTBETALING,
    FORSKUDSSKATT,
    JUSTERING,
    TREKK,
    MOTP;

    companion object {
        fun fraKode(kode: String): PosteringType {
            PosteringType.values().forEach {
                if (it.name == kode) return it
            }
            throw IllegalArgumentException("No enum constant with name=$kode")
        }
    }
}

enum class MottakerType {
    BRUKER,
    ARBG_ORG,
    ARBG_PRIV;
}
