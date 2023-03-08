package dp.oppdrag.api

import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import dp.oppdrag.repository.OppdragAlleredeSendtException
import dp.oppdrag.OppdragMapper
import dp.oppdrag.service.OppdragServiceImpl
import dp.oppdrag.model.Utbetalingsoppdrag
import dp.oppdrag.model.Utbetalingsperiode
import dp.oppdrag.utils.auth
import dp.oppdrag.utils.defaultObjectMapper
import dp.oppdrag.utils.respondConflict
import dp.oppdrag.utils.respondError
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import javax.sql.DataSource
import com.papsign.ktor.openapigen.route.path.auth.post as authPost

fun NormalOpenAPIRoute.oppdragApi(dataSource: DataSource) {
    val oppdragService = OppdragServiceImpl(dataSource)

    auth {
        route("/oppdrag") {
            authPost<Unit, String, Utbetalingsoppdrag, TokenValidationContextPrincipal?>(
                info("Oppdrag", "Send Oppdrag"),
                exampleRequest = utbetalingsoppdragExample,
                exampleResponse = "OK"
            ) { _, request ->
                Result.runCatching {
                    val oppdragMapper = OppdragMapper()
                    val oppdrag110 = oppdragMapper.tilOppdrag110(request)
                    val oppdrag = oppdragMapper.tilOppdrag(oppdrag110)

                    oppdragService.opprettOppdrag(request, oppdrag, 0)
                }.fold(
                    onFailure = {
                        if (it is OppdragAlleredeSendtException) {
                            respondConflict("Oppdrag er allerede sendt for saksnr ${request.saksnummer}.")
                        } else {
                            respondError("Klarte ikke sende oppdrag for saksnr ${request.saksnummer}")
                        }
                    },
                    onSuccess = {
                        respond("OK")
                    }
                )
            }
        }
    }
}

private val utbetalingsoppdragExample = Utbetalingsoppdrag(
    kodeEndring = Utbetalingsoppdrag.KodeEndring.NY,
    fagSystem = "",
    saksnummer = "",
    aktoer = "",
    saksbehandlerId = "",
    avstemmingTidspunkt = LocalDateTime.now(),
    utbetalingsperiode = listOf(
        Utbetalingsperiode(
            erEndringPaaEksisterendePeriode = false,
            opphoer = null,
            periodeId = 2L,
            forrigePeriodeId = 1L,
            datoForVedtak = LocalDate.now(),
            klassifisering = "",
            vedtakdatoFom = LocalDate.now(),
            vedtakdatoTom = LocalDate.now(),
            sats = BigDecimal.TEN,
            satsType = Utbetalingsperiode.SatsType.DAG,
            utbetalesTil = "",
            behandlingId = 3L,
            utbetalingsgrad = 100
        )
    ),
    gOmregning = false
)
