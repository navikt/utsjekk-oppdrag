package dp.oppdrag

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.auth.principal
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import dp.oppdrag.model.Utbetalingsoppdrag
import dp.oppdrag.model.Utbetalingsperiode
import dp.oppdrag.utils.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import no.nav.security.token.support.v2.tokenValidationSupport
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import com.papsign.ktor.openapigen.route.path.auth.get as authGet
import com.papsign.ktor.openapigen.route.path.auth.post as authPost


val authProvider = JwtProvider()

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    // Install Micrometer/Prometheus
    val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    install(MicrometerMetrics) {
        registry = appMicrometerRegistry
    }

    // Install CORS
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }

    // Install OpenAPI plugin (Swagger UI)
    install(OpenAPIGen) {
        // Serve OpenAPI definition on /openapi.json
        serveOpenApiJson = true
        // Serve Swagger UI on %swaggerUiPath%/index.html
        serveSwaggerUi = true
        swaggerUiPath = "internal/swagger-ui"
        info {
            title = "DP oppdrag API"
        }
        // Use JWT authentication (Authorize button appears in Swagger UI)
        addModules(authProvider)
    }

    // Install JSON support
    install(ContentNegotiation) {
        jackson {
            val javaTimeModule = JavaTimeModule()
            javaTimeModule.addSerializer(
                LocalDate::class.java,
                LocalDateSerializer()
            )
            javaTimeModule.addDeserializer(
                LocalDate::class.java,
                LocalDateDeserializer()
            )
            javaTimeModule.addSerializer(
                LocalDateTime::class.java,
                LocalDateTimeSerializer()
            )
            javaTimeModule.addDeserializer(
                LocalDateTime::class.java,
                LocalDateTimeDeserializer()
            )

            registerModule(javaTimeModule)
        }
    }

    // Install Authentication
    val conf = this.environment.config
    install(Authentication) {
        // Validate tokens if running on NAIS, skip validation otherwise
        if (isCurrentlyRunningOnNais()) {
            tokenValidationSupport(config = conf)
        } else {
            basic {
                skipWhen { true }
            }
        }
    }

    apiRouting {
        // Internal API
        route("/internal/liveness") {
            get<Unit, String> {
                respond("Alive")
            }
        }

        route("/internal/readyness") {
            get<Unit, String> {
                respond("Ready")
            }
        }

        route("/internal/prometheus") {
            get<Unit, String> {
                respond(appMicrometerRegistry.scrape())
            }
        }

        // API
        auth {
            route("/example/{name}") {
                authGet<StringParam, String, TokenValidationContextPrincipal?>(
                    info("String Param Endpoint", "This is a String Param Endpoint"),
                    example = "Hi"
                ) { params ->
                    val principal = principal()
                    respond("Hello, ${params.name}! Validated token " + principal?.context?.firstValidToken?.get()?.tokenAsString)
                }
            }

            route("/oppdrag") {
                authPost<Unit, String, Utbetalingsoppdrag, TokenValidationContextPrincipal?>(
                    info("Oppdrag", "Send Oppdrag"),
                    exampleRequest = Utbetalingsoppdrag(
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
                    ),
                    exampleResponse = "OK"
                ) { params, request ->
                    val oppdragMapper = OppdragMapper()
                    val oppdrag110 = oppdragMapper.tilOppdrag110(request)
                    val oppdrag = oppdragMapper.tilOppdrag(oppdrag110)

                    respond("OK")
                }
            }
        }


        route("/example/{name}") {
            // SomeParams are parameters (query or path), SomeResponse is what the backend returns and SomeRequest
            // is what was passed in the body of the request
            post<SomeParams, SomeResponse, SomeRequest> { params, request ->
                respond(SomeResponse(bar = "Hello, ${params.name}! From body: ${request.foo}."))
            }
        }
    }
}

data class StringParam(@PathParam("A simple String Param") val name: String)
data class SomeParams(@PathParam("Who to say hello to") val name: String)
data class SomeRequest(val foo: String)
data class SomeResponse(val bar: String)
