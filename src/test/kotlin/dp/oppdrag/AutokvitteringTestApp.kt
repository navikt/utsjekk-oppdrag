package dp.oppdrag

import com.ibm.mq.jms.MQQueue
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

object AutokvitteringTestApp : MessageListener {

    @JvmStatic
    fun main(args: Array<String>) {
        konfigurer()
        startDB()
        startMQ()
        lyttEtterOppdragPåKø()
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
            .withDatabaseName("dp-oppdrag")
            .withUsername("dp-bruker")
            .withPassword("dp-passord")

        psql.start()

        System.setProperty("DB_JDBC_URL", psql.jdbcUrl)
        System.setProperty("DB_USERNAME", psql.username)
        System.setProperty("DB_PASSWORD", psql.password)
    }

    private fun startMQ() {
        val queueManager = "QM1"
        val appPassord = "passw0rd"
        val mq = KGenericContainer("ibmcom/mq")
            .withEnv("LICENSE", "accept")
            .withEnv("MQ_QMGR_NAME", queueManager)
            .withEnv("MQ_APP_PASSWORD", appPassord)
            .withExposedPorts(1414, 9443)

        mq.start()

        System.setProperty("MQ_ENABLED", "true")
        System.setProperty("MQ_QUEUEMANAGER", queueManager)
        System.setProperty("MQ_PASSWORD", appPassord)
        System.setProperty("MQ_PORT", mq.getMappedPort(1414).toString())
        System.setProperty("MQ_HOSTNAME", "localhost")
        System.setProperty("MQ_CHANNEL", "DEV.APP.SVRCONN")
        System.setProperty("MQ_USER", "app")
        System.setProperty("MQ_OPPDRAG_QUEUE", "DEV.QUEUE.1")
        System.setProperty("MQ_KVITTERING_QUEUE", "DEV.QUEUE.2")
        System.setProperty("MQ_AVSTEMMING_QUEUE", "DEV.QUEUE.3")
    }

    private fun lyttEtterOppdragPåKø() {
        val queue = MQQueue(getProperty("MQ_OPPDRAG_QUEUE"))
        val queueConnection = createQueueConnection()
        val queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE)
        val queueReceiver = queueSession.createReceiver(queue)
        queueReceiver.messageListener = this
        queueConnection.start()
    }

    override fun onMessage(message: Message?) {
        val meldingTilOppdrag = (message as TextMessage).text
        val oppdrag = defaultXmlMapper.readValue(meldingTilOppdrag, Oppdrag::class.java)

        val mmel = Mmel()
        mmel.alvorlighetsgrad = "00" // OK
        oppdrag.mmel = mmel

        val queue = MQQueue(getProperty("MQ_KVITTERING_QUEUE"))
        val queueConnection = createQueueConnection()
        val queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE)
        val queueSender = queueSession.createSender(queue)

        val kvitteringXml = defaultXmlMapper.writeValueAsString(oppdrag)
        val kvittering = queueSession.createTextMessage(kvitteringXml)
        queueSender.send(kvittering)

        queueSender.close()
        queueSession.close()
        queueConnection.close()
    }
}
class KPostgreSQLContainer(imageName: String) : PostgreSQLContainer<KPostgreSQLContainer>(imageName)
class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)
