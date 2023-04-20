package dp.oppdrag.api

import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import dp.oppdrag.model.*
import dp.oppdrag.repository.SimuleringLagerRepository
import dp.oppdrag.service.SimuleringServiceImpl
import dp.oppdrag.utils.auth
import dp.oppdrag.utils.respondError
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningResponse
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import com.papsign.ktor.openapigen.route.path.auth.post as authPost

fun NormalOpenAPIRoute.simuleringApi(
    simuleringLagerRepository: SimuleringLagerRepository
) {
    val simuleringService = SimuleringServiceImpl(simuleringLagerRepository)

    auth {
        route("/simulering/etterbetalingsbelop") {
            authPost<Unit, RestSimulerResultat, Utbetalingsoppdrag, TokenValidationContextPrincipal?>(
                info("Simulering (etterbetalingsbelop)"),
                exampleRequest = utbetalingsoppdragExample,
                exampleResponse = restSimulerResultatExample
            ) { _, request ->
                Result.runCatching {
                    simuleringService.utfoerSimulering(request)
                }
                    .fold(
                        onFailure = { respondError("Simulering feilet", it) },
                        onSuccess = { respond(it) }
                    )
            }
        }

        route("/simulering/detaljert") {
            authPost<Unit, DetaljertSimuleringResultat, Utbetalingsoppdrag, TokenValidationContextPrincipal?>(
                info("Detaljert simulering"),
                exampleRequest = utbetalingsoppdragExample,
                exampleResponse = detaljertSimuleringResultatExample
            ) { _, request ->
                Result.runCatching {
                    simuleringService.utfoerSimuleringOgHentDetaljertSimuleringResultat(request)
                }
                    .fold(
                        onFailure = { respondError("Simulering feilet", it) },
                        onSuccess = { respond(it) }
                    )
            }
        }

        route("/simulering/direkte") {
            authPost<Unit, SimulerBeregningResponse, Utbetalingsoppdrag, TokenValidationContextPrincipal?>(
                info("Direkte simulering"),
                exampleRequest = utbetalingsoppdragExample,
                exampleResponse = simulerBeregningResponseExample
            ) { _, request ->
                Result.runCatching {
                    simuleringService.hentSimulerBeregningResponse(request)
                }
                    .fold(
                        onFailure = { respondError("Simulering feilet", it) },
                        onSuccess = { respond(it) }
                    )
            }
        }

        route("/simulering/feilutbetalinger") {
            authPost<Unit, List<FeilutbetaltPeriode>, HentFeilutbetalingerFraSimuleringRequest, TokenValidationContextPrincipal?>(
                info("Direkte simulering"),
                exampleRequest = hentFeilutbetalingerFraSimuleringRequestExample,
                exampleResponse = hentFeilutbetalingerResponseExample
            ) { _, request ->
                Result.runCatching {
                    simuleringService.hentFeilutbetalinger(request)
                }
                    .fold(
                        onFailure = { respondError("Simulering feilet", it) },
                        onSuccess = { respond(it) }
                    )
            }
        }
    }
}

private val utbetalingsoppdragExample = Utbetalingsoppdrag(
    kodeEndring = Utbetalingsoppdrag.KodeEndring.NY,
    fagSystem = OppdragSkjemaConstants.FAGSYSTEM,
    saksnummer = "12345",
    aktoer = "01020312345",
    saksbehandlerId = "S123456",
    avstemmingTidspunkt = LocalDateTime.now(),
    utbetalingsperiode = listOf(
        Utbetalingsperiode(
            erEndringPaaEksisterendePeriode = false,
            opphoer = null,
            periodeId = 2L,
            forrigePeriodeId = 1L,
            datoForVedtak = LocalDate.now(),
            klassifisering = "DPORAS",
            vedtakdatoFom = LocalDate.now(),
            vedtakdatoTom = LocalDate.now(),
            sats = BigDecimal.TEN,
            satsType = Utbetalingsperiode.SatsType.DAG,
            utbetalesTil = "01020312345",
            behandlingId = 3L,
            utbetalingsgrad = 100
        )
    ),
    gOmregning = false
)

private val restSimulerResultatExample = RestSimulerResultat(
    etterbetaling = 100
)

private val detaljertSimuleringResultatExample = DetaljertSimuleringResultat(
    listOf(
        SimuleringMottaker(
            simulertPostering = listOf(
                SimulertPostering(
                    fagOmraadeKode = FagOmraadeKode.DP,
                    erFeilkonto = false,
                    fom = LocalDate.now(),
                    tom = LocalDate.now().plusDays(7),
                    betalingType = BetalingType.DEBIT,
                    beloep = BigDecimal.TEN,
                    posteringType = PosteringType.YTELSE,
                    forfallsdato = LocalDate.now().plusDays(1),
                    utenInntrekk = false
                )
            ),
            mottakerNummer = "01020312345",
            mottakerType = MottakerType.ARBG_PRIV
        )
    )
)

private val simulerBeregningResponseExample = SimulerBeregningResponse()

private val hentFeilutbetalingerFraSimuleringRequestExample = HentFeilutbetalingerFraSimuleringRequest(
    eksternFagsakId = "12345",
    fagsystemsbehandlingId = "67890"
)

private val hentFeilutbetalingerResponseExample = listOf(
    FeilutbetaltPeriode(
        feilutbetaltBeloep = BigDecimal.ONE,
        fom = LocalDate.now(),
        nyttBeloep = BigDecimal.TEN,
        tidligereUtbetaltBeloep = BigDecimal.ZERO,
        tom = LocalDate.now().plusDays(1)
    )
)
