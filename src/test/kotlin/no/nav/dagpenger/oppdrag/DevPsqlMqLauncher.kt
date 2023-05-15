package no.nav.dagpenger.oppdrag

import no.nav.dagpenger.oppdrag.config.ApplicationConfig
import no.nav.dagpenger.oppdrag.iverksetting.Status
import org.springframework.boot.builder.SpringApplicationBuilder
import org.testcontainers.containers.PostgreSQLContainer

object DevPsqlMqLauncher {
    @JvmStatic
    fun main(args: Array<String>) {

        val kø = TestOppdragKø(Status.OK)
        val db = TestOppdragDb()

        SpringApplicationBuilder(ApplicationConfig::class.java)
            .profiles("dev_psql_mq")
            .properties(db.properties)
            .properties(kø.properties)
            .run(*args)
    }
}

class KPostgreSQLContainer(imageName: String) : PostgreSQLContainer<KPostgreSQLContainer>(imageName)
