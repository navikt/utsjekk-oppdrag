package dp.oppdrag

import com.ibm.mq.jms.MQQueue
import dp.oppdrag.model.OppdragStatus
import dp.oppdrag.utils.createQueueConnection
import dp.oppdrag.utils.getProperty
import io.ktor.server.netty.EngineMain
import no.trygdeetaten.skjema.oppdrag.Mmel
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import javax.jms.Message
import javax.jms.MessageListener
import javax.jms.Session
import javax.jms.TextMessage

object AutokvitteringTestApp {

    @JvmStatic
    fun main(args: Array<String>) {
        konfigurer()
        startDB()
        TestOppdragKø(OppdragStatus.OK)
        EngineMain.main(args)
    }

    private fun konfigurer() {
        System.setProperty(
            "AZURE_APP_WELL_KNOWN_URL",
            "https://login.microsoftonline.com/77678b69-1daf-47b6-9072-771d270ac800/v2.0/.well-known/openid-configuration"
        )
    }

    private fun startDB() {
        val psql = KPostgreSQLContainer("postgres")
        psql.start()

        System.setProperty("DB_JDBC_URL", psql.jdbcUrl)
        System.setProperty("DB_USERNAME", psql.username)
        System.setProperty("DB_PASSWORD", psql.password)
    }
}
class KPostgreSQLContainer(imageName: String) : PostgreSQLContainer<KPostgreSQLContainer>(imageName)
class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)
