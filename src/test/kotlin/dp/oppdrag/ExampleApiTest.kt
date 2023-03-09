package dp.oppdrag

import com.nimbusds.jwt.SignedJWT
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import kotlin.test.Test
import kotlin.test.assertEquals

class ExampleApiTest : TestBase() {

    @Test
    fun shouldGet200WithToken() = setUpTestApplication {
        val token: SignedJWT = mockOAuth2Server.issueToken(ISSUER_ID, "someclientid", DefaultOAuth2TokenCallback())
        val response = client.get("/example/Ola") {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${token.serialize()}")
            }
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Hello, Ola! Validated token " + token.serialize(), response.bodyAsText())
    }

    @Test
    fun shouldGet401WithoutToken() = setUpTestApplication {
        val response = client.get("/example/Ola")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals("", response.bodyAsText())
    }
}
