package dp.oppdrag.api

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext
import com.papsign.ktor.openapigen.route.route
import dp.oppdrag.mapper.OppdragMapper
import dp.oppdrag.model.*
import dp.oppdrag.model.OppdragSkjemaConstants.Companion.FAGSYSTEM
import dp.oppdrag.repository.OppdragAlleredeSendtException
import dp.oppdrag.repository.OppdragLagerRepository
import dp.oppdrag.service.OppdragService
import dp.oppdrag.service.OppdragServiceImpl
import dp.oppdrag.utils.*
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import com.papsign.ktor.openapigen.route.path.auth.post as authPost

fun NormalOpenAPIRoute.oppdragApi(oppdragLagerRepository: OppdragLagerRepository) {
    val oppdragService = OppdragServiceImpl(oppdragLagerRepository)

    auth {
        route("/oppdrag") {
            authPost<Unit, String, Utbetalingsoppdrag, TokenValidationContextPrincipal?>(
                info("Send Oppdrag"),
                exampleRequest = utbetalingsoppdragExample,
                exampleResponse = "OK"
            ) { _, request ->
                sendOppdrag(oppdragService, request, 0)
            }
        }

        route("/oppdragPaaNytt/{versjon}") {
            authPost<OppdragPaaNyttParams, String, Utbetalingsoppdrag, TokenValidationContextPrincipal?>(
                info("Send Oppdrag på nytt"),
                exampleRequest = utbetalingsoppdragExample,
                exampleResponse = "OK"
            ) { params, request ->
                sendOppdrag(oppdragService, request, params.versjon)
            }
        }

        route("/status") {
            authPost<Unit, KvitteringDto, OppdragId, TokenValidationContextPrincipal?>(
                info("Hent oppdragsstatus"),
                exampleRequest = oppdragIdExample,
                exampleResponse = KvitteringDto(OppdragLagerStatus.KVITTERT_OK)
            ) { _, request ->
                Result.runCatching {
                    oppdragService.hentStatusForOppdrag(request)
                }.fold(
                    onFailure = {
                        respondNotFound("Fant ikke oppdrag med $request")
                    },
                    onSuccess = {
                        respondOk(KvitteringDto(it.status, it.kvitteringsmelding?.beskrMelding))
                    }
                )
            }
        }
    }
}

private suspend inline fun <reified TResponse : Any> OpenAPIPipelineResponseContext<TResponse>.sendOppdrag(
    oppdragService: OppdragService,
    request: Utbetalingsoppdrag,
    versjon: Int
) {
    Result.runCatching {
        val oppdrag = OppdragMapper().tilOppdrag(request)

        oppdragService.opprettOppdrag(request, oppdrag, versjon)
    }.fold(
        onFailure = {
            if (it is OppdragAlleredeSendtException) {
                respondConflict("Oppdrag er allerede sendt for " +
                        "person_ident = ${request.aktoer.subSequence(0,6).padEnd(11, '*')}, " +
                        "behandling_id = ${request.behandlingsIdForFoersteUtbetalingsperiode()}, " +
                        "fagsystem = ${request.fagSystem}, " +
                        "versjon = $versjon")
            } else {
                respondError("Klarte ikke sende oppdrag for saksnr ${request.saksnummer}", it)
            }
        },
        onSuccess = {
            respondOk("OK")
        }
    )
}

data class OppdragPaaNyttParams(@PathParam("Versjon") val versjon: Int)

private val utbetalingsoppdragExample = Utbetalingsoppdrag(
    kodeEndring = Utbetalingsoppdrag.KodeEndring.NY,
    fagSystem = FAGSYSTEM,
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
            behandlingId = "3",
            utbetalingsgrad = 100
        )
    ),
    gOmregning = false
)

private val oppdragIdExample = OppdragId(
    fagsystem = FAGSYSTEM,
    personIdent = "01020312345",
    behandlingsId = "3"
)
