package no.nav.utsjekk.oppdrag

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.containers.PostgreSQLContainer

class PostgreSQLInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    companion object {
        val container: GenericPostgreSQLContainer =
            GenericPostgreSQLContainer("postgres:latest")
                .withDatabaseName("utsjekk-oppdrag")
                .withUsername("postgres")
                .withPassword("test")
                .withExposedPorts(5432)
    }

    override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
        TestPropertyValues.of(
            "spring.datasource.url=" + container.jdbcUrl,
            "spring.datasource.username=" + container.username,
            "spring.datasource.password=" + container.password,
        ).applyTo(configurableApplicationContext.environment)
    }
}

class GenericPostgreSQLContainer(imageName: String) : PostgreSQLContainer<GenericPostgreSQLContainer>(imageName)
