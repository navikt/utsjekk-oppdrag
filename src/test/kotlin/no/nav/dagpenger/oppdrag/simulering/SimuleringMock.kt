package no.nav.dagpenger.oppdrag.simulering

import no.nav.system.os.eksponering.simulerfpservicewsbinding.SimulerFpService
import no.nav.system.os.entiteter.beregningskjema.Beregning
import no.nav.system.os.entiteter.beregningskjema.BeregningStoppnivaa
import no.nav.system.os.entiteter.beregningskjema.BeregningStoppnivaaDetaljer
import no.nav.system.os.entiteter.beregningskjema.BeregningsPeriode
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SendInnOppdragRequest
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SendInnOppdragResponse
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningRequest
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Service
@Profile("local")
class SimuleringServiceMock(simulerFpService: SimulerFpService) : SimuleringService(simulerFpService) {
    override fun simuler(request: SimuleringRequestBody) = lagTestSimuleringResponse().tilSimulering()
}

@Configuration
class SimuleringMock {
    @Bean
    fun simulerFpService() =
        object : SimulerFpService {
            override fun sendInnOppdrag(p0: SendInnOppdragRequest?) =
                SendInnOppdragResponse()

            override fun simulerBeregning(p0: SimulerBeregningRequest?) =
                lagTestSimuleringResponse()
        }
}

private fun lagTestSimuleringResponse(): SimulerBeregningResponse {
    val currentDate: LocalDate = LocalDate.now()

    val enTideligereMåned = currentDate.plusMonths(1)

    val periodeNåværendeMåned = lagBeregningsPeriode(
        listOf(lagBeregningStoppniva(currentDate)),
        currentDate,
    )

    val periodeTidligereMåned = lagBeregningsPeriode(
        listOf(lagBeregningStoppniva(enTideligereMåned, 2)),
        enTideligereMåned,
    )

    return lagSimulerBeregningResponse(listOf(periodeNåværendeMåned, periodeTidligereMåned))
}

private fun lagSimulerBeregningResponse(beregningsPerioder: List<BeregningsPeriode>): SimulerBeregningResponse {
    val beregning = Beregning()
    beregning.beregningsPeriode.addAll(beregningsPerioder)

    val simulerBeregningResponse =
        no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.SimulerBeregningResponse()
    simulerBeregningResponse.simulering = beregning

    val response = SimulerBeregningResponse()
    response.response = simulerBeregningResponse

    return response
}

private fun lagBeregningsPeriode(bergeningStopNiva: List<BeregningStoppnivaa>, date: LocalDate): BeregningsPeriode {
    val beregningsPeriode = BeregningsPeriode()
    beregningsPeriode.periodeFom = date.with(TemporalAdjusters.firstDayOfMonth()).toString()
    beregningsPeriode.periodeTom = date.with(TemporalAdjusters.lastDayOfMonth()).toString()
    beregningsPeriode.beregningStoppnivaa.addAll(bergeningStopNiva)

    return beregningsPeriode
}

private fun lagBeregningStoppniva(
    date: LocalDate,
    forfall: Long = 0,
    fagOmrade: String = "BA",
): BeregningStoppnivaa {
    val beregningStoppnivaa = BeregningStoppnivaa()
    beregningStoppnivaa.forfall = date.plusDays(forfall).toString()
    beregningStoppnivaa.kodeFagomraade = fagOmrade

    beregningStoppnivaa.beregningStoppnivaaDetaljer.add(lagBeregningStoppnivaaDetaljer(dato = date))
    beregningStoppnivaa.utbetalesTilId = "1234567890"

    return beregningStoppnivaa
}

private fun lagBeregningStoppnivaaDetaljer(
    posteringType: String = "YTELSE",
    belop: BigDecimal = BigDecimal(1000),
    dato: LocalDate? = null,
): BeregningStoppnivaaDetaljer {
    val beregningStoppnivaaDetaljer = BeregningStoppnivaaDetaljer()
    beregningStoppnivaaDetaljer.typeKlasse = posteringType
    beregningStoppnivaaDetaljer.belop = belop
    beregningStoppnivaaDetaljer.faktiskFom = dato?.toString()
    beregningStoppnivaaDetaljer.faktiskTom = dato?.plusMonths(1).toString()
    return beregningStoppnivaaDetaljer
}
