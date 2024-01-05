package no.nav.dagpenger.simulering.simulering

import no.nav.system.os.entiteter.beregningskjema.Beregning
import no.nav.system.os.entiteter.beregningskjema.BeregningStoppnivaa
import no.nav.system.os.entiteter.beregningskjema.BeregningStoppnivaaDetaljer
import no.nav.system.os.entiteter.beregningskjema.BeregningsPeriode
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SendInnOppdragRequest
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SendInnOppdragResponse
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningRequest
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningResponse
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerFpService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.SimulerBeregningResponse as InnerSimulerBeregningResponse

@Service
@Profile("local")
class SimuleringServiceMock(simulerFpService: SimulerFpService) : SimuleringService(simulerFpService) {
    override fun simuler(request: SimuleringRequestBody) =
        enTestSimuleringResponse(SimuleringRequestBuilder(request).build()).tilSimulering()
}

@Configuration
class SimuleringMock {
    @Bean
    fun simulerFpService() =
        object : SimulerFpService {
            override fun sendInnOppdrag(p0: SendInnOppdragRequest?) = SendInnOppdragResponse()

            override fun simulerBeregning(p0: SimulerBeregningRequest) = enTestSimuleringResponse(p0)
        }
}

private fun enTestSimuleringResponse(request: SimulerBeregningRequest): SimulerBeregningResponse {
    val periodeNåværendeMåned =
        LocalDate.now().let {
            enBeregningsPeriode(listOf(enBeregningStoppniva(it)), it)
        }

    val periodeNesteMåned =
        LocalDate.now().plusMonths(1).let {
            enBeregningsPeriode(listOf(enBeregningStoppniva(it, 2)), it)
        }

    return enSimulerBeregningResponse(request, listOf(periodeNåværendeMåned, periodeNesteMåned))
}

private fun enSimulerBeregningResponse(
    request: SimulerBeregningRequest,
    beregningsPerioder: List<BeregningsPeriode>,
) = SimulerBeregningResponse().apply {
    this.response =
        InnerSimulerBeregningResponse().apply {
            simulering =
                Beregning().apply {
                    gjelderId = request.request.oppdrag.oppdragGjelderId
                    gjelderNavn = "Navn Navnesen"
                    datoBeregnet = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
                    belop = BigDecimal(1234)
                    beregningsPeriode.addAll(beregningsPerioder)
                }
        }
}

private fun enBeregningsPeriode(
    bergeningStopNiva: List<BeregningStoppnivaa>,
    date: LocalDate,
) = BeregningsPeriode().apply {
    periodeFom = date.with(TemporalAdjusters.firstDayOfMonth()).toString()
    periodeTom = date.with(TemporalAdjusters.lastDayOfMonth()).toString()
    beregningStoppnivaa.addAll(bergeningStopNiva)
}

private fun enBeregningStoppniva(
    dato: LocalDate,
    forfall: Long = 0,
    fagområde: String = "BA",
) = BeregningStoppnivaa().apply {
    fagsystemId = "en-fagsystem-id"
    utbetalesTilId = "1234567890"
    utbetalesTilNavn = "Navn Navnesen"
    this.forfall = dato.plusDays(forfall).toString()
    kodeFagomraade = fagområde
    beregningStoppnivaaDetaljer.add(enBeregningStoppnivaaDetaljer(dato = dato))
}

private fun enBeregningStoppnivaaDetaljer(
    posteringType: String = "YTELSE",
    beløp: BigDecimal = BigDecimal(1000),
    dato: LocalDate? = null,
) = BeregningStoppnivaaDetaljer().apply {
    faktiskFom = dato?.toString()
    faktiskTom = dato?.plusMonths(1).toString()
    kontoStreng = "en-konto"
    belop = beløp
    sats = BigDecimal(1234)
    typeSats = "en-satstype"
    antallSats = BigDecimal(1)
    uforeGrad = BigInteger.valueOf(100)
    klassekode = "en-klassekode"
    typeKlasse = posteringType
}
