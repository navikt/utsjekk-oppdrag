package no.nav.dagpenger.oppdrag

import com.ibm.mq.jms.MQQueue
import com.ibm.mq.jms.MQQueueConnectionFactory
import com.ibm.msg.client.wmq.WMQConstants
import no.nav.dagpenger.oppdrag.iverksetting.Jaxb
import no.nav.dagpenger.oppdrag.iverksetting.Status
import no.nav.dagpenger.oppdrag.util.Containers
import no.trygdeetaten.skjema.oppdrag.Mmel
import java.io.Closeable
import java.util.Properties
import javax.jms.Message
import javax.jms.MessageListener
import javax.jms.QueueConnection
import javax.jms.Session
import javax.jms.TextMessage

class TestOppdragKø(private val kvitteringStatus: Status, private val kvitteringsmelding: String? = null) :
    MessageListener,
    Closeable {

    val properties = Properties()
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
        properties["OPPDRAG_MQ_PORT_OVERRIDE"] = mq.getMappedPort(1414).toString()
        properties["oppdrag.mq.port"] = mq.getMappedPort(1414).toString()
        properties["oppdrag.mq.queuemanager"] = queueManager
        properties["oppdrag.mq.send"] = "DEV.QUEUE.1"
        properties["oppdrag.mq.mottak"] = "DEV.QUEUE.2"
        properties["oppdrag.mq.avstemming"] = "DEV.QUEUE.3"
        properties["oppdrag.mq.tss"] = "DEV.QUEUE.4"
        properties["oppdrag.mq.channel"] = "DEV.ADMIN.SVRCONN"
        properties["oppdrag.mq.hostname"] = "localhost"
        properties["oppdrag.mq.user"] = "admin"
        properties["oppdrag.mq.password"] = appPassord
        properties["oppdrag.mq.enabled"] = true
    }

    private fun lyttEtterOppdragPåKø() {
        val queue = MQQueue(properties.getProperty("oppdrag.mq.send"))
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

        val queue = MQQueue(properties.getProperty("oppdrag.mq.mottak"))
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
        qcf.hostName = properties.getProperty("oppdrag.mq.hostname")
        qcf.port = properties.getProperty("oppdrag.mq.port")?.toInt() ?: 0
        qcf.channel = properties.getProperty("oppdrag.mq.channel")
        qcf.transportType = WMQConstants.WMQ_CM_CLIENT
        qcf.queueManager = properties.getProperty("oppdrag.mq.queuemanager")

        return qcf.createQueueConnection(properties.getProperty("oppdrag.mq.user"), properties.getProperty("oppdrag.mq.password"))
    }
}
