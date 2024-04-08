package no.nav.utsjekk.oppdrag

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import java.io.Closeable

class TestOppdragDb : ApplicationContextInitializer<ConfigurableApplicationContext>, Closeable {
    override fun close() {
        PostgreSQLInitializer.container.close()
    }

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        PostgreSQLInitializer.container.start()

        TestPropertyValues.of(
            "spring.datasource.url=${PostgreSQLInitializer.container.jdbcUrl}",
            "spring.datasource.username=${PostgreSQLInitializer.container.username}",
            "spring.datasource.password=${PostgreSQLInitializer.container.password}",
        ).applyTo(applicationContext.environment)
    }
}
