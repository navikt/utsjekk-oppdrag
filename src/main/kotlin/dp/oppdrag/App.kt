package dp.oppdrag

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.auth.principal
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import dp.oppdrag.api.avstemmingApi
import dp.oppdrag.api.internalApi
import dp.oppdrag.api.oppdragApi
import dp.oppdrag.listener.OppdragListenerMQ
import dp.oppdrag.repository.OppdragLagerRepositoryJdbc
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
import mu.KotlinLogging
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import no.nav.security.token.support.v2.tokenValidationSupport
import org.flywaydb.core.Flyway
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import javax.sql.DataSource
import com.papsign.ktor.openapigen.route.path.auth.get as authGet

val defaultLogger = KotlinLogging.logger {}
val defaultAuthProvider = JwtProvider()
val defaultObjectMapper: ObjectMapper = ObjectMapper()
    .registerKotlinModule()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
val defaultXmlMapper: ObjectMapper = XmlMapper.builder()
    .defaultUseWrapper(false)
    .addModules(JaxbAnnotationModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .serializationInclusion(JsonInclude.Include.NON_EMPTY)
    .defaultDateFormat(SimpleDateFormat("yyyy-MM-dd"))
    .build()
lateinit var defaultDataSource: DataSource

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    // Create DataSource and run migrations
    prepareDataSource()

    // Create repository object
    val oppdragLagerRepository = OppdragLagerRepositoryJdbc(defaultDataSource)

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
        addModules(defaultAuthProvider)
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

    // Listen to receipt queue
    OppdragListenerMQ(oppdragLagerRepository)

    apiRouting {
        internalApi(appMicrometerRegistry)

        oppdragApi(oppdragLagerRepository)
        avstemmingApi(oppdragLagerRepository)

        // Example API
        // Will be deleted soon
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
        }
    }
}

fun prepareDataSource() {
    if (!::defaultDataSource.isInitialized) {
        val url = "jdbc:postgresql://" + getProperty("DB_HOST") +
                ":" + getProperty("DB_PORT") +
                "/" + getProperty("DB_DATABASE")

        defaultDataSource = HikariDataSource().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = url
            username = getProperty("DB_USERNAME")
            password = getProperty("DB_PASSWORD")
            connectionTimeout = 10000 // 10s
            maxLifetime = 30000 // 30s
            keepaliveTime = 20000 // 20s
            maximumPoolSize = 5
        }
    }

    val flyway = Flyway.configure()
        .connectRetries(20)
        .dataSource(defaultDataSource)
        .load()
    flyway.migrate()
}

// Example API classes
// Will be deleted soon
data class StringParam(@PathParam("A simple String Param") val name: String)
