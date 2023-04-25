package dp.oppdrag.api

import com.zaxxer.hikari.HikariDataSource
import dp.oppdrag.AutokvitteringTestApp
import dp.oppdrag.KGenericContainer
import dp.oppdrag.defaultDataSource
import dp.oppdrag.module
import io.ktor.server.config.*
import io.ktor.server.testing.*
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
open class TestBase {

    companion object {
        const val ISSUER_ID = "default"
        const val REQUIRED_AUDIENCE = "default"

        var mockOAuth2Server = MockOAuth2Server()

        @Container
        private val dbContainer: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:14-alpine")

        @BeforeAll
        @JvmStatic
        fun setup() {
            mockOAuth2Server = MockOAuth2Server()
            mockOAuth2Server.start(8091)

            defaultDataSource = HikariDataSource().apply {
                jdbcUrl = dbContainer.jdbcUrl
                username = dbContainer.username
                password = dbContainer.password

                validate()
            }
        }

        @AfterAll
        @JvmStatic
        fun cleanup() {
            mockOAuth2Server.shutdown()
        }
    }

    private fun setOidcConfig(): MapApplicationConfig {
        return MapApplicationConfig(
            "no.nav.security.jwt.issuers.size" to "1",
            "no.nav.security.jwt.issuers.0.issuer_name" to ISSUER_ID,
            "no.nav.security.jwt.issuers.0.discoveryurl" to mockOAuth2Server.wellKnownUrl(ISSUER_ID).toString(),
            "no.nav.security.jwt.issuers.0.accepted_audience" to REQUIRED_AUDIENCE
        )
    }

    fun setUpTestApplication(block: suspend ApplicationTestBuilder.() -> Unit) {
        testApplication {
            environment {
                config = setOidcConfig()
            }
            application {
                module()
            }

            block()
        }
    }
}
