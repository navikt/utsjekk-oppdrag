package no.nav.dagpenger.oppdrag

import no.nav.dagpenger.oppdrag.config.ApplicationConfig
import org.springframework.boot.builder.SpringApplicationBuilder

object LocalLauncher {

    @JvmStatic
    fun main(args: Array<String>) {
        val app = SpringApplicationBuilder(ApplicationConfig::class.java)
            .profiles("local")
        app.run(*args)
    }
}
