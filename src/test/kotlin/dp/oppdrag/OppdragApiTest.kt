package dp.oppdrag

import com.nimbusds.jwt.SignedJWT
import dp.oppdrag.model.Utbetalingsoppdrag
import dp.oppdrag.model.Utbetalingsperiode
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class OppdragApiTest : TestBase() {

    @Test
    fun shouldGet200FirstTimeAnd409SecondTime() = setUpTestApplication {
        val utbetalingsoppdrag = Utbetalingsoppdrag(
            kodeEndring = Utbetalingsoppdrag.KodeEndring.NY,
            fagSystem = "EFOG",
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

        val token: SignedJWT = mockOAuth2Server.issueToken(ISSUER_ID, "someclientid", DefaultOAuth2TokenCallback())

        val response1 = client.post("/oppdrag") {
            headers {
                append(HttpHeaders.ContentType, "application/json")
                append(HttpHeaders.Authorization, "Bearer ${token.serialize()}")
            }
            setBody(defaultObjectMapper.writeValueAsString(utbetalingsoppdrag))
        }

        assertEquals(HttpStatusCode.OK, response1.status)
        assertEquals("OK", response1.bodyAsText())

        val response2 = client.post("/oppdrag") {
            headers {
                append(HttpHeaders.ContentType, "application/json")
                append(HttpHeaders.Authorization, "Bearer ${token.serialize()}")
            }
            setBody(defaultObjectMapper.writeValueAsString(utbetalingsoppdrag))
        }

        assertEquals(HttpStatusCode.Conflict, response2.status)
        assertEquals("Oppdrag er allerede sendt for saksnr 12345", response2.bodyAsText())
    }

    @Test
    fun shouldGet401WithoutToken() = setUpTestApplication {
        val response = client.post("/oppdrag") {
            headers {
                append(HttpHeaders.ContentType, "application/json")
            }
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals("", response.bodyAsText())
    }
}
