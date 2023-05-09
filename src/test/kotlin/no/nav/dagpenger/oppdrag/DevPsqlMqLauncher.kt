package no.nav.dagpenger.oppdrag

import no.nav.dagpenger.oppdrag.config.ApplicationConfig
import no.nav.dagpenger.oppdrag.iverksetting.Status
import org.springframework.boot.builder.SpringApplicationBuilder
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import java.util.Properties

object DevPsqlMqLauncher {
    @JvmStatic
    fun main(args: Array<String>) {

        val psql = KPostgreSQLContainer("postgres")
            .withDatabaseName("dp-oppdrag")
            .withUsername("postgres")
            .withPassword("test")

        psql.start()
        /*
                val mq = KGenericContainer("ibmcom/mq")
                    .withEnv("LICENSE", "accept")
                    .withEnv("MQ_QMGR_NAME", "QM1")
                    .withExposedPorts(1414, 9443)

                mq.start()
        */
        val properties = Properties()
        properties["SPRING_DATASOURCE_URL_OVERRIDE"] = psql.jdbcUrl
        properties["SPRING_DATASOURCE_USERNAME_OVERRIDE"] = psql.username
        properties["SPRING_DATASOURCE_PASSWORD_OVERRIDE"] = psql.password
        properties["SPRING_DATASOURCE_DRIVER_OVERRIDE"] = "org.postgresql.Driver"

        TestOppdragKø(Status.OK)

        SpringApplicationBuilder(ApplicationConfig::class.java)
            .profiles("dev_psql_mq")
            .properties(properties)
            .run(*args)
    }
}

class KPostgreSQLContainer(imageName: String) : PostgreSQLContainer<KPostgreSQLContainer>(imageName)

class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)
