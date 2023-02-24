package dp.oppdrag

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


class App {

}

fun main() {
    embeddedServer(Netty, host = "localhost", port = 8080, module = Application::module).start(wait = true)
}

fun Application.module() {
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }

    routing {
        swaggerUI(path = "internal/swagger", swaggerFile = "openapi/documentation.yaml") {

        }

        get("/") {
            call.respondText("Hello, world!")
        }
    }
}
