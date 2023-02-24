package dp.oppdrag

import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.security.token.support.v2.tokenValidationSupport


fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    // Install CORS
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }

    // Install OpenAPI plugin
    install(OpenAPIGen) {
        // Serve OpenAPI definition on /openapi.json
        serveOpenApiJson = true
        // Serve Swagger UI on %swaggerUiPath%/index.html
        serveSwaggerUi = true
        swaggerUiPath = "internal/swagger-ui"
        info {
            title = "Minimal Example API"
        }
    }

    // Install JSON support
    install(ContentNegotiation) {
        jackson()
    }

    // Install Authentication
    val conf = this.environment.config
    println("#########################")
    println(conf.configList("no.nav.security.jwt.issuers"))
    install(Authentication) {
        if (isCurrentlyRunningOnNais()) {
            tokenValidationSupport(config = conf)
        } else {
            basic {
                skipWhen { true }
            }
        }
    }

    apiRouting {
        route("/") {
            get<Any, String> {
                respond("Hello, world!")
            }
        }

        route("/{name}") {
            // SomeParams are parameters (query or path), SomeResponse is what the backend returns
            get<SomeParams, SomeResponse> { params ->
                respond(SomeResponse(bar = "Hello ${params.name}!"))
            }
        }

        route("/example/{name}") {
            // SomeParams are parameters (query or path), SomeResponse is what the backend returns and SomeRequest
            // is what was passed in the body of the request
            post<SomeParams, SomeResponse, SomeRequest> { params, someRequest ->
                respond(SomeResponse(bar = "Hello ${params.name}! From body: ${someRequest.foo}."))
            }
        }
    }
}

fun isCurrentlyRunningOnNais(): Boolean {
    return System.getenv("NAIS_APP_NAME") != null
}

data class SomeParams(@PathParam("who to say hello") val name: String)
data class SomeRequest(val foo: String)
data class SomeResponse(val bar: String)