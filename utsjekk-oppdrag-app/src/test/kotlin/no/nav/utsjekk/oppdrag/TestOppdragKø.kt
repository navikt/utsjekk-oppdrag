package no.nav.utsjekk.oppdrag

import com.ibm.mq.jakarta.jms.MQQueue
import com.ibm.mq.jakarta.jms.MQQueueConnectionFactory
import com.ibm.msg.client.jakarta.wmq.WMQConstants
import jakarta.jms.Message
import jakarta.jms.MessageListener
import jakarta.jms.QueueConnection
import jakarta.jms.Session
import jakarta.jms.TextMessage
import no.nav.utsjekk.oppdrag.iverksetting.domene.Kvitteringstatus
import no.nav.utsjekk.oppdrag.iverksetting.mq.OppdragXmlMapper
import no.trygdeetaten.skjema.oppdrag.Mmel
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import java.io.Closeable
import java.lang.IllegalStateException

internal class TestOppdragKø(private val kvitteringStatus: Kvitteringstatus, private val kvitteringsmelding: String? = null) :
    MessageListener,
    ApplicationContextInitializer<ConfigurableApplicationContext>,
    Closeable {
    private val mq = GenericMQContainer("ibmcom/mq")
    private lateinit var queueConnection: QueueConnection

    private fun startMQ(context: ConfigurableApplicationContext) {
        val port =
            context.environment.getProperty("oppdrag.mq.port")?.toInt()
                ?: throw IllegalStateException("Fant ikke port for MQ i config")

        mq
            .withEnv("LICENSE", "accept")
            .withEnv("MQ_QMGR_NAME", context.environment.getProperty("oppdrag.mq.queuemanager"))
            .withEnv("MQ_APP_PASSWORD", context.environment.getProperty("oppdrag.mq.password"))
            .withEnv("persistance.enabled", "true")
            .withExposedPorts(port)
            .start()

        System.setProperty("oppdrag.mq.mottak", "DEV.QUEUE.1")
        TestPropertyValues.of(
            "oppdrag.mq.port=${mq.getMappedPort(port)}",
        ).applyTo(context.environment)
    }

    private fun lyttEtterOppdragPåKø(context: ConfigurableApplicationContext) {
        val queue = MQQueue(context.environment.getProperty("oppdrag.mq.send"))
        queueConnection = createQueueConnection(context)
        val queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE)
        val queueReceiver = queueSession.createReceiver(queue)
        queueReceiver.messageListener = this
        queueConnection.start()
    }

    override fun onMessage(message: Message?) {
        val meldingTilOppdrag = (message as TextMessage).text
        val oppdrag = OppdragXmlMapper.tilOppdrag(meldingTilOppdrag)

        val mmel = Mmel()
        mmel.alvorlighetsgrad = kvitteringStatus.kode
        kvitteringsmelding?.let {
            mmel.beskrMelding = it
        }
        oppdrag.mmel = mmel

        val queue = MQQueue(System.getProperty("oppdrag.mq.mottak"))
        val queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE)
        val queueSender = queueSession.createSender(queue)

        val kvitteringXml = OppdragXmlMapper.tilXml(oppdrag)
        val kvittering = queueSession.createTextMessage(kvitteringXml)
        queueSender.send(kvittering)

        queueSender.close()
        queueSession.close()
    }

    override fun close() {
        queueConnection.close()
        mq.close()
    }

    private fun createQueueConnection(context: ConfigurableApplicationContext): QueueConnection {
        val qcf = MQQueueConnectionFactory()
        qcf.hostName = context.environment.getProperty("oppdrag.mq.hostname")
        qcf.port = context.environment.getProperty("oppdrag.mq.port")?.toInt()
            ?: throw IllegalStateException("Fant ikke MQ-port i config")
        qcf.channel = context.environment.getProperty("oppdrag.mq.channel")
        qcf.transportType = WMQConstants.WMQ_CM_CLIENT
        qcf.queueManager = context.environment.getProperty("oppdrag.mq.queuemanager")

        return qcf.createQueueConnection(
            context.environment.getProperty("oppdrag.mq.user"),
            context.environment.getProperty("oppdrag.mq.password"),
        )
    }

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        startMQ(applicationContext)
        lyttEtterOppdragPåKø(applicationContext)
    }
}
