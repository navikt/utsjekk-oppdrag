package no.nav.dagpenger.oppdrag

import no.nav.dagpenger.oppdrag.util.Containers.postgreSQLContainer
import java.io.Closeable
import java.util.Properties

class TestOppdragDb : Closeable {

    val properties = Properties()

    init {
        postgreSQLContainer.start()
        properties["SPRING_DATASOURCE_URL_OVERRIDE"] = postgreSQLContainer.jdbcUrl
        properties["SPRING_DATASOURCE_USERNAME_OVERRIDE"] = postgreSQLContainer.username
        properties["SPRING_DATASOURCE_PASSWORD_OVERRIDE"] = postgreSQLContainer.password
        properties["SPRING_DATASOURCE_DRIVER_OVERRIDE"] = "org.postgresql.Driver"
    }

    override fun close() {
        postgreSQLContainer.close()
    }
}
