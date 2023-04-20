package dp.oppdrag.mapper

import dp.oppdrag.model.RestSimulerResultat
import no.nav.system.os.entiteter.beregningskjema.BeregningsPeriode
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningResponse
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SimulerBeregningResponseMapper {
    fun toRestSimulerResult(
        response: SimulerBeregningResponse,
        dato: LocalDate = LocalDate.now()
    ): RestSimulerResultat {

        val totalEtterbetalingsBeloep =
            response.response?.simulering?.beregningsPeriode?.sumOf { finnEtterbetalingPerPeriode(it, dato) }
                ?: 0

        return RestSimulerResultat(etterbetaling = totalEtterbetalingsBeloep)
    }

    private fun finnEtterbetalingPerPeriode(
        beregningsPeriode: BeregningsPeriode,
        dato: LocalDate,
        kodeFagomraade: String = "BA"
    ): Int {
        // Fremtidige perioder gir ingen etterbetaling.
        val datoFraPeriode = LocalDate.parse(beregningsPeriode.periodeFom, DateTimeFormatter.ISO_DATE)
        if (datoFraPeriode > dato) return 0

        val stoppNivaBA =
            beregningsPeriode.beregningStoppnivaa.filter { it.kodeFagomraade?.trim() == kodeFagomraade }

        // Feilutbetaling medfører at etterbetaling er 0
        val inneholderFeilutbetalingType =
            stoppNivaBA.any { stopNivaa -> stopNivaa.beregningStoppnivaaDetaljer.any { detaljer -> detaljer.typeKlasse?.trim() == TypeKlasse.FEIL.name } }
        if (inneholderFeilutbetalingType) return 0

        // Summer perioder av type YTEL og med forfallsdato bak i tiden.
        return stoppNivaBA.filter { forfallPassert(it.forfall, dato) }
            .flatMap { it.beregningStoppnivaaDetaljer }
            .filter { it.typeKlasse?.trim() == TypeKlasse.YTEL.name }
            .sumOf { it.belop?.toInt() ?: 0 }
    }

    private fun forfallPassert(forfall: String, dato: LocalDate): Boolean =
        dato >= LocalDate.parse(forfall, DateTimeFormatter.ISO_DATE)
}


enum class TypeKlasse {
    FEIL,
    YTEL
}
