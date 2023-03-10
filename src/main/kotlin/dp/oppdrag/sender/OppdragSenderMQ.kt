package dp.oppdrag.sender

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.ibm.mq.constants.CMQC
import com.ibm.mq.jms.MQQueue
import com.ibm.mq.jms.MQQueueConnection
import com.ibm.mq.jms.MQQueueConnectionFactory
import com.ibm.mq.jms.MQQueueSender
import com.ibm.msg.client.jms.JmsConstants
import com.ibm.msg.client.wmq.common.CommonConstants
import dp.oppdrag.defaultLogger
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import javax.jms.Session


class OppdragSenderMQ : OppdragSender {

    private val UTF_8_WITH_PUA = 1208

    override fun sendOppdrag(oppdrag: Oppdrag): String {
        val xmlMapper = XmlMapper()

        val oppdragId = oppdrag.oppdrag110?.oppdragsLinje150?.lastOrNull()?.henvisning
        val oppdragXml = xmlMapper.writeValueAsString(oppdrag)

        if (!System.getenv("MQ_ENABLED").toBoolean()) {
            defaultLogger.info { "MQ-integrasjon mot oppdrag er skrudd av" }
            return ""
        }

        defaultLogger.info {
            "Sender oppdrag for fagsystem=${oppdrag.oppdrag110.kodeFagomraade} og " +
                    "fagsak=${oppdrag.oppdrag110.fagsystemId} behandling=$oppdragId til Oppdragsystemet"
        }

        try {
            val connectionFactory = MQQueueConnectionFactory()
            connectionFactory.hostName = System.getenv("MQ_HOSTNAME")
            connectionFactory.queueManager = System.getenv("MQ_QUEUEMANAGER")
            connectionFactory.channel = System.getenv("MQ_CHANNEL")
            connectionFactory.port = System.getenv("MQ_PORT").toInt()
            connectionFactory.transportType = CommonConstants.WMQ_CM_CLIENT
            connectionFactory.ccsid = UTF_8_WITH_PUA
            connectionFactory.setIntProperty(JmsConstants.JMS_IBM_ENCODING, CMQC.MQENC_NATIVE)
            connectionFactory.setBooleanProperty(JmsConstants.USER_AUTHENTICATION_MQCSP, true)
            connectionFactory.setIntProperty(JmsConstants.JMS_IBM_CHARACTER_SET, UTF_8_WITH_PUA)

            val connection = connectionFactory.createQueueConnection(
                System.getenv("MQ_USER"),
                System.getenv("MQ_PASSWORD")
            ) as MQQueueConnection
            val session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE)
            val queue = session.createQueue(System.getenv("MQ_QUEUE")) as MQQueue
            val sender = session.createSender(queue) as MQQueueSender

            val message = session.createTextMessage(oppdragXml)
            message.jmsReplyTo = MQQueue(System.getenv("MQ_MOTTAK")) // kvitteringsKø

            connection.start()
            sender.send(message)
            sender.close()
            session.close()
            connection.close()
        } catch (e: Exception) {
            defaultLogger.error { "Klarte ikke sende Oppdrag til OS. Feil: $e" }
            throw e
        }

        return oppdrag.oppdrag110.fagsystemId
    }
}
