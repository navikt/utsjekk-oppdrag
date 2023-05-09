package no.nav.dagpenger.oppdrag

import com.ibm.mq.jms.MQQueue
import com.ibm.mq.jms.MQQueueConnectionFactory
import com.ibm.msg.client.wmq.WMQConstants
import no.nav.dagpenger.oppdrag.iverksetting.Jaxb
import no.nav.dagpenger.oppdrag.iverksetting.Status
import no.trygdeetaten.skjema.oppdrag.Mmel
import java.io.Closeable
import javax.jms.Message
import javax.jms.MessageListener
import javax.jms.QueueConnection
import javax.jms.Session
import javax.jms.TextMessage

class TestOppdragKø(private val kvitteringStatus: Status, private val kvitteringsmelding: String? = null): MessageListener,
    Closeable {

    private lateinit var mq: KGenericContainer
    init {
        startMQ()
        lyttEtterOppdragPåKø()
    }

    private fun startMQ() {
        mq = KGenericContainer("ibmcom/mq")
            .withEnv("LICENSE", "accept")
            .withExposedPorts(1414, 9443)

        mq.start()
        System.setProperty("OPPDRAG_MQ_PORT_OVERRIDE", mq.getMappedPort(1414).toString())
    }


    private fun lyttEtterOppdragPåKø() {
        val queue = MQQueue(System.getenv("oppdrag.mq.send"))
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

        val queue = MQQueue(System.getenv("oppdrag.mq.mottak"))
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
        qcf.hostName = System.getenv("oppdrag.mq.hostname")
        qcf.port = System.getenv("oppdrag.mq.port")?.toInt() ?: 0
        qcf.channel = System.getenv("oppdrag.mq.channel")
        qcf.transportType = WMQConstants.WMQ_CM_CLIENT
        qcf.queueManager = System.getenv("oppdrag.mq.queuemanager")

        return qcf.createQueueConnection(System.getenv("oppdrag.mq.user"), System.getenv("oppdrag.mq.password"))
    }
}