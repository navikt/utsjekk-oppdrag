package dp.oppdrag

import com.ibm.mq.jms.MQQueue
import dp.oppdrag.model.OppdragStatus
import dp.oppdrag.utils.createQueueConnection
import dp.oppdrag.utils.getProperty
import io.ktor.utils.io.core.*
import no.trygdeetaten.skjema.oppdrag.Mmel
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import javax.jms.Message
import javax.jms.MessageListener
import javax.jms.Session
import javax.jms.TextMessage

class TestOppdragKø(private val kvitteringStatus: OppdragStatus, private val kvitteringsmelding: String? = null): MessageListener, Closeable {

    private lateinit var mq: KGenericContainer
    init {
        startMQ()
        lyttEtterOppdragPåKø()
    }

    private fun startMQ() {
        val queueManager = "QM1"
        val appPassord = "passw0rd"
        mq = KGenericContainer("ibmcom/mq")
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
        mmel.alvorlighetsgrad = kvitteringStatus.kode
        kvitteringsmelding?.let {
            mmel.beskrMelding = it
        }
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

    override fun close() {
        mq.close()
    }

}