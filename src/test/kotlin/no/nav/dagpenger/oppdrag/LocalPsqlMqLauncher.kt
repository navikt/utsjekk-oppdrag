package no.nav.dagpenger.oppdrag

import no.nav.dagpenger.oppdrag.config.ApplicationConfig
import no.nav.dagpenger.oppdrag.iverksetting.Status
import org.springframework.boot.builder.SpringApplicationBuilder
import org.testcontainers.containers.PostgreSQLContainer

object LocalPsqlMqLauncher {
    @JvmStatic
    fun main(args: Array<String>) {

        TestOppdragKø(Status.OK)
        val db = TestOppdragDb()

        SpringApplicationBuilder(ApplicationConfig::class.java)
            .profiles("local")
            .properties(db.properties)
            .run(*args)
    }
}

class KPostgreSQLContainer(imageName: String) : PostgreSQLContainer<KPostgreSQLContainer>(imageName)
