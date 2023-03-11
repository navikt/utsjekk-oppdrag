package dp.oppdrag.sender

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.ibm.mq.constants.CMQC
import com.ibm.mq.jms.MQQueue
import com.ibm.msg.client.jms.JmsConstants
import com.ibm.msg.client.jms.JmsFactoryFactory
import com.ibm.msg.client.wmq.WMQConstants
import dp.oppdrag.defaultLogger
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import javax.jms.*


class OppdragSenderMQ : OppdragSender {

    private val UTF_8_WITH_PUA = 1208
    private lateinit var producer: MessageProducer
    private lateinit var connection: Connection
    private lateinit var session: Session
    private lateinit var destination: Destination

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
            // Create a connection factory
            val ff = JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER)
            val cf = ff.createConnectionFactory()

            // Set the properties
            cf.setStringProperty(WMQConstants.WMQ_HOST_NAME, System.getenv("MQ_HOSTNAME"))
            cf.setIntProperty(WMQConstants.WMQ_PORT, System.getenv("MQ_PORT").toInt())
            cf.setStringProperty(WMQConstants.WMQ_CHANNEL, System.getenv("MQ_CHANNEL"))
            cf.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT)
            cf.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, System.getenv("MQ_QUEUEMANAGER"))
            cf.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true)
            cf.setStringProperty(WMQConstants.USERID, System.getenv("MQ_USER"))
            cf.setStringProperty(WMQConstants.PASSWORD, System.getenv("MQ_PASSWORD"))
            cf.setIntProperty(JmsConstants.JMS_IBM_ENCODING, CMQC.MQENC_NATIVE)
            cf.setIntProperty(JmsConstants.JMS_IBM_CHARACTER_SET, UTF_8_WITH_PUA)

            // Create JMS objects
            connection = cf.createConnection()
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
            destination = session.createQueue(System.getenv("MQ_QUEUE"))
            producer = session.createProducer(destination)

            // Start the connection
            connection.start()

            // Create a message
            val message = session.createTextMessage(oppdragXml)
            message.jmsReplyTo = MQQueue(System.getenv("MQ_MOTTAK")) // kvitteringsKø

            // Send the message
            producer.send(message)

        } catch (e: Exception) {
            defaultLogger.error { "Klarte ikke sende Oppdrag til OS. Feil: $e" }
            throw e
        } finally {
            if (::producer.isInitialized) {
                try {
                    producer.close();
                } catch (jmsex: JMSException) {
                    defaultLogger.warn { "Producer could not be closed." }
                }
            }

            if (::session.isInitialized) {
                try {
                    session.close();
                } catch (jmsex: JMSException) {
                    defaultLogger.warn { "Session could not be closed." }
                }
            }

            if (::connection.isInitialized) {
                try {
                    connection.close();
                } catch (jmsex: JMSException) {
                    defaultLogger.warn { "Connection could not be closed." }
                }
            }
        }

        return oppdrag.oppdrag110.fagsystemId
    }
}
