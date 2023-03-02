package dp.oppdrag

import com.nimbusds.jwt.SignedJWT
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import kotlin.test.Test
import kotlin.test.assertEquals

class AppTest : TestBase() {

    @Test
    fun shouldGetAliveWithoutToken() = setUpTestApplication {
        val response = client.get("/internal/liveness")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Alive", response.bodyAsText())
    }

    @Test
    fun shouldGetReadyWithoutToken() = setUpTestApplication {
        val response = client.get("/internal/readyness")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Ready", response.bodyAsText())
    }

    @Test
    fun shouldGet200WithToken() = setUpTestApplication {
        val token: SignedJWT = mockOAuth2Server.issueToken(ISSUER_ID, "someclientid", DefaultOAuth2TokenCallback())
        val response = client.get("/Ola") {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${token.serialize()}")
            }
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Hello, Ola! Validated token " + token.serialize(), response.bodyAsText())
    }

    @Test
    fun shouldGet401WithoutToken() = setUpTestApplication {
        val response = client.get("/Ola")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals("", response.bodyAsText())
    }
}
