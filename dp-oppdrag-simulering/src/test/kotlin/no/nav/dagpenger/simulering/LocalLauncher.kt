package no.nav.dagpenger.simulering

import no.nav.dagpenger.simulering.config.ApplicationConfig
import org.springframework.boot.builder.SpringApplicationBuilder

object LocalLauncher {
    @JvmStatic
    fun main(args: Array<String>) {
        val app =
            SpringApplicationBuilder(ApplicationConfig::class.java)
                .profiles("local")
        app.run(*args)
    }
}
