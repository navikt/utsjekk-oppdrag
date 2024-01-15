package no.nav.dagpenger.oppdrag

import no.nav.dagpenger.oppdrag.config.ApplicationConfig
import no.nav.dagpenger.oppdrag.iverksetting.domene.Kvitteringstatus
import org.springframework.boot.builder.SpringApplicationBuilder

object LocalPsqlMqLauncher {
    @JvmStatic
    fun main(args: Array<String>) {
        SpringApplicationBuilder(ApplicationConfig::class.java)
            .profiles("local")
            .initializers(TestOppdragKø(Kvitteringstatus.OK), TestOppdragDb())
            .run(*args)
    }
}
