package no.nav.dagpenger.oppdrag

import no.nav.dagpenger.oppdrag.config.ApplicationConfig
import no.nav.dagpenger.oppdrag.util.Containers
import org.springframework.boot.builder.SpringApplicationBuilder
import java.util.Properties

object LocalLauncher {

    @JvmStatic
    fun main(args: Array<String>) {
        val properties = Properties()
        properties["SPRING_DATASOURCE_URL_OVERRIDE"] = "jdbc:postgresql://0.0.0.0:5432/dp-oppdrag"
        properties["SPRING_DATASOURCE_USERNAME_OVERRIDE"] = "postgres"
        properties["SPRING_DATASOURCE_PASSWORD_OVERRIDE"] = "test"


        val app = SpringApplicationBuilder(ApplicationConfig::class.java)
            .profiles("local")
        app.run(*args)
    }
}
