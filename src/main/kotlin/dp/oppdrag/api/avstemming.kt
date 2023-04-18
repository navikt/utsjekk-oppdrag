package dp.oppdrag.api

import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.route
import dp.oppdrag.model.*
import dp.oppdrag.repository.MellomlagringKonsistensavstemmingRepository
import dp.oppdrag.repository.OppdragLagerRepository
import dp.oppdrag.service.GrensesnittavstemmingServiceImpl
import dp.oppdrag.service.KonsistensavstemmingServiceImpl
import dp.oppdrag.utils.*
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import java.time.LocalDateTime
import java.util.*
import com.papsign.ktor.openapigen.route.path.auth.post as authPost

fun NormalOpenAPIRoute.avstemmingApi(
    oppdragLagerRepository: OppdragLagerRepository,
    mellomlagringKonsistensavstemmingRepository: MellomlagringKonsistensavstemmingRepository
) {
    val grensesnittavstemmingService = GrensesnittavstemmingServiceImpl(oppdragLagerRepository)
    val konsistensavstemmingService = KonsistensavstemmingServiceImpl(
        oppdragLagerRepository,
        mellomlagringKonsistensavstemmingRepository
    )

    auth {
        route("/grensesnittavstemming") {
            authPost<Unit, String, GrensesnittavstemmingRequest, TokenValidationContextPrincipal?>(
                info("Grensesnittavstemming"),
                exampleRequest = grensesnittavstemmingRequestExample,
                exampleResponse = "OK"
            ) { _, request ->
                Result.runCatching {
                    grensesnittavstemmingService.utfoerGrensesnittavstemming(request)
                }
                    .fold(
                        onFailure = { respondError("Grensesnittavstemming feilet", it) },
                        onSuccess = { respondOk("Grensesnittavstemming sendt ok") }
                    )
            }
        }

        route("/konsistensavstemming") {
            authPost<KonsistensavstemmingParams, String, KonsistensavstemmingRequest, TokenValidationContextPrincipal?>(
                info("Konsistensavstemming"),
                exampleRequest = konsistensavstemmingRequestExample,
                exampleResponse = "OK"
            ) { params, request ->
                Result.runCatching {
                    konsistensavstemmingService.utfoerKonsistensavstemming(
                        request,
                        params.sendStartmelding,
                        params.sendAvsluttmelding,
                        params.transaksjonsId
                    )
                }
                    .fold(
                        onFailure = { respondError("Konsistensavstemming feilet", it) },
                        onSuccess = { respondOk("Konsistensavstemming sendt ok") }
                    )
            }
        }
    }
}

data class KonsistensavstemmingParams(
    @QueryParam("Send startmelding") val sendStartmelding: Boolean,
    @QueryParam("Send avsluttmelding") val sendAvsluttmelding: Boolean,
    @QueryParam("Transaksjons ID") val transaksjonsId: UUID?
)

private val grensesnittavstemmingRequestExample = GrensesnittavstemmingRequest(
    fra = LocalDateTime.now(),
    til = LocalDateTime.now().plusDays(1)
)

private val konsistensavstemmingRequestExample = KonsistensavstemmingRequest(
    perioderForBehandlinger = listOf(
        PerioderForBehandling(
            behandlingId = "12345",
            perioder = setOf(1L, 2L, 3L),
            aktivFoedselsnummer = "01020312345"
        )
    ),
    avstemmingstidspunkt = LocalDateTime.now()
)
