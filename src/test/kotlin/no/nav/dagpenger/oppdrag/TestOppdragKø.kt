package no.nav.dagpenger.oppdrag

import com.ibm.mq.jakarta.jms.MQQueue
import com.ibm.mq.jakarta.jms.MQQueueConnectionFactory
import com.ibm.msg.client.jakarta.wmq.WMQConstants
import jakarta.jms.Message
import jakarta.jms.MessageListener
import jakarta.jms.QueueConnection
import jakarta.jms.Session
import jakarta.jms.TextMessage
import no.nav.dagpenger.oppdrag.iverksetting.Jaxb
import no.nav.dagpenger.oppdrag.iverksetting.Status
import no.nav.dagpenger.oppdrag.util.Containers
import no.trygdeetaten.skjema.oppdrag.Mmel
import java.io.Closeable
import java.util.Properties

class TestOppdragKø(private val kvitteringStatus: Status, private val kvitteringsmelding: String? = null) :
    MessageListener,
    Closeable {

    private val queueManager = "QM1"
    private val appPassord = "passw0rd"

    var mq = Containers.MyGeneralContainer("ibmcom/mq")
        .withEnv("LICENSE", "accept")
        .withEnv("MQ_QMGR_NAME", queueManager)
        .withEnv("MQ_APP_PASSWORD", appPassord)
        .withEnv("persistance.enabled", "true")
        .withExposedPorts(1414)

    init {
        startMQ()
        lyttEtterOppdragPåKø()
    }

    private fun startMQ() {
        mq.start()
        System.setProperty("oppdrag.mq.port", mq.getMappedPort(1414).toString())
        System.setProperty("oppdrag.mq.queuemanager", queueManager)
        System.setProperty("oppdrag.mq.send", "DEV.QUEUE.1")
        System.setProperty("oppdrag.mq.mottak", "DEV.QUEUE.2")
        System.setProperty("oppdrag.mq.avstemming", "DEV.QUEUE.3")
        System.setProperty("oppdrag.mq.channel","DEV.ADMIN.SVRCONN")
        System.setProperty("oppdrag.mq.hostname","localhost")
        System.setProperty("oppdrag.mq.user", "admin")
        System.setProperty("oppdrag.mq.password", appPassord)
        System.setProperty("oppdrag.mq.enabled", true.toString())
    }

    private fun lyttEtterOppdragPåKø() {
        val queue = MQQueue(System.getProperty("oppdrag.mq.send"))
        val queueConnection = createQueueConnection()
        val queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE)
        val queueReceiver = queueSession.createReceiver(queue)
        queueReceiver.messageListener = this
        queueConnection.start()
    }

    override fun onMessage(message: Message?) {
        val meldingTilOppdrag = (message as TextMessage).text
        val oppdrag = Jaxb.tilOppdrag(meldingTilOppdrag)

        val mmel = Mmel()
        mmel.alvorlighetsgrad = kvitteringStatus.kode
        kvitteringsmelding?.let {
            mmel.beskrMelding = it
        }
        oppdrag.mmel = mmel

        val queue = MQQueue(System.getProperty("oppdrag.mq.mottak"))
        val queueConnection = createQueueConnection()
        val queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE)
        val queueSender = queueSession.createSender(queue)

        val kvitteringXml = Jaxb.tilXml(oppdrag)
        val kvittering = queueSession.createTextMessage(kvitteringXml)
        queueSender.send(kvittering)

        queueSender.close()
        queueSession.close()
        queueConnection.close()
    }

    override fun close() {
        mq.close()
    }

    fun createQueueConnection(): QueueConnection {
        val qcf = MQQueueConnectionFactory()
        qcf.hostName = System.getProperty("oppdrag.mq.hostname")
        qcf.port = System.getProperty("oppdrag.mq.port")?.toInt() ?: 0
        qcf.channel = System.getProperty("oppdrag.mq.channel")
        qcf.transportType = WMQConstants.WMQ_CM_CLIENT
        qcf.queueManager = System.getProperty("oppdrag.mq.queuemanager")

        return qcf.createQueueConnection(System.getProperty("oppdrag.mq.user"), System.getProperty("oppdrag.mq.password"))
    }
}
