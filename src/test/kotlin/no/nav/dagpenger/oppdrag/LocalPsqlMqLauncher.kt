package no.nav.dagpenger.oppdrag

import no.nav.dagpenger.oppdrag.config.ApplicationConfig
import no.nav.dagpenger.oppdrag.iverksetting.Status
import org.springframework.boot.builder.SpringApplicationBuilder

object LocalPsqlMqLauncher {
    @JvmStatic
    fun main(args: Array<String>) {

        SpringApplicationBuilder(ApplicationConfig::class.java)
            .profiles("local")
            .initializers(TestOppdragKø(Status.OK), TestOppdragDb())
            .run(*args)
    }
}
