package dp.oppdrag.api

import com.nimbusds.jwt.SignedJWT
import dp.oppdrag.defaultObjectMapper
import dp.oppdrag.model.GrensesnittavstemmingRequest
import dp.oppdrag.model.KonsistensavstemmingRequest
import dp.oppdrag.model.PerioderForBehandling
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class AvstemmingApiTest : TestBase() {

    @Test
    fun shouldGet200WhenGrensesnittavstemming() = setUpTestApplication {

        val grensesnittavstemmingRequest = GrensesnittavstemmingRequest(
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(1)
        )

        val token: SignedJWT = mockOAuth2Server.issueToken(ISSUER_ID, "someclientid", DefaultOAuth2TokenCallback())

        val response = client.post("/grensesnittavstemming") {
            headers {
                append(HttpHeaders.ContentType, "application/json")
                append(HttpHeaders.Authorization, "Bearer ${token.serialize()}")
            }
            setBody(defaultObjectMapper.writeValueAsString(grensesnittavstemmingRequest))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Grensesnittavstemming sendt ok", response.bodyAsText())

    }

    @Test
    fun shouldGet401WhenGrensesnittavstemmingWithoutToken() = setUpTestApplication {
        val response = client.post("/grensesnittavstemming") {
            headers {
                append(HttpHeaders.ContentType, "application/json")
            }
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals("", response.bodyAsText())
    }

    @Test
    fun shouldGet500WhenKonsistensavstemmingWithoutMQ() = setUpTestApplication {

        System.setProperty("MQ_ENABLED", "false")

        val konsistensavstemmingRequest = KonsistensavstemmingRequest(
            listOf(
                PerioderForBehandling(
                    behandlingId = "12345",
                    perioder = setOf(1L, 2L, 3L),
                    aktivFoedselsnummer = "01020312345"
                )
            ),
            LocalDateTime.now()
        )

        val token: SignedJWT = mockOAuth2Server.issueToken(ISSUER_ID, "someclientid", DefaultOAuth2TokenCallback())

        val response = client.post("/konsistensavstemming?sendStartmelding=true&sendAvsluttmelding=true") {
            headers {
                append(HttpHeaders.ContentType, "application/json")
                append(HttpHeaders.Authorization, "Bearer ${token.serialize()}")
            }
            setBody(defaultObjectMapper.writeValueAsString(konsistensavstemmingRequest))
        }

        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertEquals("Konsistensavstemming feilet", response.bodyAsText())
    }

    @Test
    fun shouldGet401WhenKonsistensavstemmingWithoutToken() = setUpTestApplication {
        val response = client.post("/konsistensavstemming") {
            headers {
                append(HttpHeaders.ContentType, "application/json")
            }
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals("", response.bodyAsText())
    }
}
