package no.nav.dagpenger.oppdrag

import no.nav.dagpenger.oppdrag.util.Containers.postgreSQLContainer
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import java.io.Closeable

class TestOppdragDb : ApplicationContextInitializer<ConfigurableApplicationContext>, Closeable {

    override fun close() {
        postgreSQLContainer.close()
    }

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        postgreSQLContainer.start()

        TestPropertyValues.of(
            "spring.datasource.url=${postgreSQLContainer.jdbcUrl}",
            "spring.datasource.username=${postgreSQLContainer.username}",
            "spring.datasource.password=${postgreSQLContainer.password}"
        ).applyTo(applicationContext.environment)
    }
}
