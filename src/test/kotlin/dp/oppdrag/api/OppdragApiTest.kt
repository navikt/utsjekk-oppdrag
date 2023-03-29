package dp.oppdrag.api

import com.nimbusds.jwt.SignedJWT
import dp.oppdrag.defaultObjectMapper
import dp.oppdrag.model.OppdragId
import dp.oppdrag.model.OppdragLagerStatus
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

        val utbetalingsoppdrag = opprettUtbetalingsoppdrag(1L)

        val token: SignedJWT = mockOAuth2Server.issueToken(ISSUER_ID, "someclientid", DefaultOAuth2TokenCallback())

        // Send Oppdrag
        val response1 = client.post("/oppdrag") {
            headers {
                append(HttpHeaders.ContentType, "application/json")
                append(HttpHeaders.Authorization, "Bearer ${token.serialize()}")
            }
            setBody(defaultObjectMapper.writeValueAsString(utbetalingsoppdrag))
        }

        assertEquals(HttpStatusCode.OK, response1.status)
        assertEquals("OK", response1.bodyAsText())

        // Send Oppdrag again
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
    fun shouldGet500WhenAnotherPSQLException() = setUpTestApplication {
        val tooBigSakId = (1..51).joinToString("") { "A" }
        val utbetalingsoppdrag = opprettUtbetalingsoppdrag(1L, tooBigSakId)

        val token: SignedJWT = mockOAuth2Server.issueToken(ISSUER_ID, "someclientid", DefaultOAuth2TokenCallback())

        // Send Oppdrag
        val response = client.post("/oppdrag") {
            headers {
                append(HttpHeaders.ContentType, "application/json")
                append(HttpHeaders.Authorization, "Bearer ${token.serialize()}")
            }
            setBody(defaultObjectMapper.writeValueAsString(utbetalingsoppdrag))
        }

        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertEquals("Klarte ikke sende oppdrag for saksnr $tooBigSakId", response.bodyAsText())
    }

    @Test
    fun shouldGet401WhenSendOppdragWithoutToken() = setUpTestApplication {
        val response = client.post("/oppdrag") {
            headers {
                append(HttpHeaders.ContentType, "application/json")
            }
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals("", response.bodyAsText())
    }

    @Test
    fun shouldSendOppdragPaaNytt() = setUpTestApplication {

        val utbetalingsoppdrag = opprettUtbetalingsoppdrag(2L)

        val token: SignedJWT = mockOAuth2Server.issueToken(ISSUER_ID, "someclientid", DefaultOAuth2TokenCallback())

        // Send Oppdrag
        val response1 = client.post("/oppdrag") {
            headers {
                append(HttpHeaders.ContentType, "application/json")
                append(HttpHeaders.Authorization, "Bearer ${token.serialize()}")
            }
            setBody(defaultObjectMapper.writeValueAsString(utbetalingsoppdrag))
        }

        assertEquals(HttpStatusCode.OK, response1.status)
        assertEquals("OK", response1.bodyAsText())

        // Send Oppdrag again but with the same version
        val response2 = client.post("/oppdragPaaNytt/0") {
            headers {
                append(HttpHeaders.ContentType, "application/json")
                append(HttpHeaders.Authorization, "Bearer ${token.serialize()}")
            }
            setBody(defaultObjectMapper.writeValueAsString(utbetalingsoppdrag))
        }

        assertEquals(HttpStatusCode.Conflict, response2.status)
        assertEquals("Oppdrag er allerede sendt for saksnr 12345", response2.bodyAsText())

        // Send Oppdrag again with another version
        val response3 = client.post("/oppdragPaaNytt/1") {
            headers {
                append(HttpHeaders.ContentType, "application/json")
                append(HttpHeaders.Authorization, "Bearer ${token.serialize()}")
            }
            setBody(defaultObjectMapper.writeValueAsString(utbetalingsoppdrag))
        }

        assertEquals(HttpStatusCode.OK, response3.status)
        assertEquals("OK", response3.bodyAsText())
    }

    @Test
    fun shouldGet401WhenSendOppdragPaaNyttWithoutToken() = setUpTestApplication {
        val response = client.post("/oppdragPaaNytt/1") {
            headers {
                append(HttpHeaders.ContentType, "application/json")
            }
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals("", response.bodyAsText())
    }

    @Test
    fun shouldGet404ForNonexistentOppdragAndStatusForExistingOppdrag() = setUpTestApplication {
        val behandlingsId = 3L
        val utbetalingsoppdrag = opprettUtbetalingsoppdrag(behandlingsId)
        val oppdragId = OppdragId(
            fagsystem = "DP",
            personIdent = "01020312345",
            behandlingsId = behandlingsId.toString()
        )

        val token: SignedJWT = mockOAuth2Server.issueToken(ISSUER_ID, "someclientid", DefaultOAuth2TokenCallback())

        // Get status before sending Oppdrag
        val response1 = client.post("/status") {
            headers {
                append(HttpHeaders.ContentType, "application/json")
                append(HttpHeaders.Authorization, "Bearer ${token.serialize()}")
            }
            setBody(defaultObjectMapper.writeValueAsString(oppdragId))
        }

        assertEquals(HttpStatusCode.NotFound, response1.status)
        assertEquals(
            "Fant ikke oppdrag med OppdragId(fagsystem=DP, behandlingsId=$behandlingsId)",
            response1.bodyAsText()
        )

        // Send Oppdrag
        val response2 = client.post("/oppdrag") {
            headers {
                append(HttpHeaders.ContentType, "application/json")
                append(HttpHeaders.Authorization, "Bearer ${token.serialize()}")
            }
            setBody(defaultObjectMapper.writeValueAsString(utbetalingsoppdrag))
        }

        assertEquals(HttpStatusCode.OK, response2.status)
        assertEquals("OK", response2.bodyAsText())

        // Get status after sending Oppdrag
        val response3 = client.post("/status") {
            headers {
                append(HttpHeaders.ContentType, "application/json")
                append(HttpHeaders.Authorization, "Bearer ${token.serialize()}")
            }
            setBody(defaultObjectMapper.writeValueAsString(oppdragId))
        }

        assertEquals(HttpStatusCode.OK, response3.status)
        assertEquals(OppdragLagerStatus.LAGT_PAA_KOE.name, response3.bodyAsText())
    }

    @Test
    fun shouldGet401WhenGetStatusWithoutToken() = setUpTestApplication {
        val oppdragId = OppdragId(
            fagsystem = "DP",
            personIdent = "01020312345",
            behandlingsId = "3"
        )

        val response = client.post("/status") {
            headers {
                append(HttpHeaders.ContentType, "application/json")
            }
            setBody(defaultObjectMapper.writeValueAsString(oppdragId))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals("", response.bodyAsText())
    }

    private fun opprettUtbetalingsoppdrag(behandlingId: Long, saksnummer: String = "12345"): Utbetalingsoppdrag {
        return Utbetalingsoppdrag(
            kodeEndring = Utbetalingsoppdrag.KodeEndring.NY,
            fagSystem = "DP",
            saksnummer = saksnummer,
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
                    behandlingId = behandlingId,
                    utbetalingsgrad = 100
                )
            ),
            gOmregning = false
        )
    }
}
