package dp.oppdrag.service

import dp.oppdrag.defaultLogger
import dp.oppdrag.defaultXmlMapper
import dp.oppdrag.mapper.SimulerBeregningRequestMapper
import dp.oppdrag.mapper.SimulerBeregningResponseMapper
import dp.oppdrag.mapper.SimuleringResultatTransformer
import dp.oppdrag.mapper.TypeKlasse
import dp.oppdrag.model.*
import dp.oppdrag.repository.SimuleringLagerRepository
import dp.oppdrag.sender.SimuleringSenderImpl
import no.nav.system.os.eksponering.simulerfpservicewsbinding.SimulerBeregningFeilUnderBehandling
import no.nav.system.os.entiteter.beregningskjema.Beregning
import no.nav.system.os.entiteter.beregningskjema.BeregningStoppnivaaDetaljer
import no.nav.system.os.entiteter.beregningskjema.BeregningsPeriode
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningRequest
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningResponse
import java.math.BigDecimal
import java.time.LocalDate

class SimuleringServiceImpl(private val simuleringLagerRepository: SimuleringLagerRepository) : SimuleringService {

    private val simuleringSender = SimuleringSenderImpl()
    private val simulerBeregningRequestMapper = SimulerBeregningRequestMapper()
    private val simulerBeregningResponseMapper = SimulerBeregningResponseMapper()
    private val simuleringResultatTransformer = SimuleringResultatTransformer()
    override fun utfoerSimulering(utbetalingsoppdrag: Utbetalingsoppdrag): RestSimulerResultat {
        val response = hentSimulerBeregningResponse(utbetalingsoppdrag)
        return simulerBeregningResponseMapper.toRestSimulerResult(response)
    }

    override fun hentSimulerBeregningResponse(utbetalingsoppdrag: Utbetalingsoppdrag): SimulerBeregningResponse {
        val simulerBeregningRequest = simulerBeregningRequestMapper.tilSimulerBeregningRequest(utbetalingsoppdrag)

        return hentSimulerBeregningResponse(simulerBeregningRequest)
    }

    private fun hentSimulerBeregningResponse(simulerBeregningRequest: SimulerBeregningRequest): SimulerBeregningResponse {
        try {
            return simuleringSender.hentSimulerBeregningResponse(simulerBeregningRequest)
        } catch (ex: SimulerBeregningFeilUnderBehandling) {
            val feilmelding = genererFeilmelding(ex)
            throw Exception(feilmelding, ex)
        } catch (ex: Exception) {
            defaultLogger.error("Feil mot simulering", ex)
            throw Exception("Ukjent feil mot simulering", ex)
        }
    }

    override fun utfoerSimuleringOgHentDetaljertSimuleringResultat(utbetalingsoppdrag: Utbetalingsoppdrag): DetaljertSimuleringResultat {
        val simulerBeregningRequest = simulerBeregningRequestMapper.tilSimulerBeregningRequest(utbetalingsoppdrag)

        val simuleringsLager = SimuleringLager.lagFraOppdrag(utbetalingsoppdrag, simulerBeregningRequest)
        simuleringLagerRepository.lagreINyTransaksjon(simuleringsLager)

        val respons = hentSimulerBeregningResponse(simulerBeregningRequest)

        simuleringsLager.responseXml = defaultXmlMapper.writeValueAsString(respons)
        simuleringLagerRepository.oppdater(simuleringsLager)

        val beregning = respons.response?.simulering ?: return DetaljertSimuleringResultat(emptyList())
        return simuleringResultatTransformer.mapSimulering(
            beregning = beregning,
            utbetalingsoppdrag = utbetalingsoppdrag
        )
    }

    override fun hentFeilutbetalinger(request: HentFeilutbetalingerFraSimuleringRequest): List<FeilutbetaltPeriode> {
        val simuleringLager = simuleringLagerRepository.hentSisteSimuleringsresultat(
            request.eksternFagsakId,
            request.fagsystemsbehandlingId
        )
        val respons = defaultXmlMapper.readValue(simuleringLager.responseXml!!, SimulerBeregningResponse::class.java)
        val simulering = respons.response.simulering

        val feilPosteringerMedPositivBeloep = finnFeilPosteringer(simulering)
        val alleYtelPosteringer = finnYtelPosteringer(simulering)

        val feilutbetaltPerioder = feilPosteringerMedPositivBeloep.map { feilPostering ->
            val periode = feilPostering.key
            val feilutbetaltBeloep = feilPostering.value.sumOf { it.belop }
            val ytelPosteringerForPeriode = hentYtelPerioder(periode, alleYtelPosteringer)
            FeilutbetaltPeriode(
                fom = LocalDate.parse(periode.periodeFom),
                tom = LocalDate.parse(periode.periodeTom),
                feilutbetaltBeloep = feilutbetaltBeloep,
                tidligereUtbetaltBeloep = summerNegativeYtelPosteringer(
                    ytelPosteringerForPeriode,
                    alleYtelPosteringer
                ).abs(),
                nyttBeloep = summerPostiveYtelPosteringer(
                    ytelPosteringerForPeriode,
                    alleYtelPosteringer
                ) - feilutbetaltBeloep
            )
        }

        return feilutbetaltPerioder
    }

    private fun finnFeilPosteringer(simulering: Beregning): Map<BeregningsPeriode, List<BeregningStoppnivaaDetaljer>> {
        return simulering.beregningsPeriode.map { beregningsperiode ->
            beregningsperiode to beregningsperiode.beregningStoppnivaa.map { stoppNivaa ->
                stoppNivaa.beregningStoppnivaaDetaljer.filter { detalj ->
                    detalj.typeKlasse == TypeKlasse.FEIL.name && detalj.belop > BigDecimal.ZERO
                }
            }.flatten()
        }.filter { it.second.isNotEmpty() }.toMap()
    }

    private fun finnYtelPosteringer(simulering: Beregning): Map<BeregningsPeriode, List<BeregningStoppnivaaDetaljer>> {
        return simulering.beregningsPeriode.associateWith { beregningsperiode ->
            beregningsperiode.beregningStoppnivaa.map { stoppNivaa ->
                stoppNivaa.beregningStoppnivaaDetaljer.filter { detalj ->
                    detalj.typeKlasse == TypeKlasse.YTEL.name
                }
            }.flatten()
        }
    }

    private fun hentYtelPerioder(
        feilutbetaltePeriode: BeregningsPeriode,
        ytelPerioder: Map<BeregningsPeriode, List<BeregningStoppnivaaDetaljer>>
    ): List<BeregningsPeriode> {
        return ytelPerioder.keys.filter { ytelPeriode ->
            ytelPeriode.periodeFom == feilutbetaltePeriode.periodeFom &&
                    ytelPeriode.periodeTom == feilutbetaltePeriode.periodeTom
        }
    }

    private fun summerNegativeYtelPosteringer(
        perioder: List<BeregningsPeriode>,
        ytelPerioder: Map<BeregningsPeriode, List<BeregningStoppnivaaDetaljer>>
    ) =
        perioder.sumOf { beregningsperiode ->
            ytelPerioder.getValue(beregningsperiode).filter { it.belop < BigDecimal.ZERO }
                .sumOf { detalj -> detalj.belop }
        }

    private fun summerPostiveYtelPosteringer(
        perioder: List<BeregningsPeriode>,
        ytelPerioder: Map<BeregningsPeriode, List<BeregningStoppnivaaDetaljer>>
    ) =
        perioder.sumOf { beregningsperiode ->
            ytelPerioder.getValue(beregningsperiode).filter { it.belop > BigDecimal.ZERO }
                .sumOf { detalj -> detalj.belop }
        }

    private fun genererFeilmelding(ex: SimulerBeregningFeilUnderBehandling): String =
        ex.faultInfo.let {
            "Feil ved hentSimulering (SimulerBeregningFeilUnderBehandling) " +
                    "source: ${it.errorSource}, " +
                    "type: ${it.errorType}, " +
                    "message: ${it.errorMessage}, " +
                    "rootCause: ${it.rootCause}, " +
                    "rootCause: ${it.dateTimeStamp}"
        }
}
