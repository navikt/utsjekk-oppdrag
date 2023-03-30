package dp.oppdrag.sender

import com.ibm.mq.jms.MQQueue
import dp.oppdrag.defaultLogger
import dp.oppdrag.defaultXmlMapper
import dp.oppdrag.utils.createQueueConnection
import dp.oppdrag.utils.getProperty
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import javax.jms.*


class OppdragSenderMQ : OppdragSender {

    private lateinit var queueConnection: QueueConnection
    private lateinit var queueSession: QueueSession
    private lateinit var queueSender: QueueSender

    override fun sendOppdrag(oppdrag: Oppdrag): String {
        val oppdragId = oppdrag.oppdrag110?.oppdragsLinje150?.lastOrNull()?.henvisning
        val oppdragXml = defaultXmlMapper.writeValueAsString(oppdrag)

        if (!getProperty("MQ_ENABLED").toBoolean()) {
            defaultLogger.info { "MQ-integrasjon mot oppdrag er skrudd av" }
            return ""
        }

        defaultLogger.info {
            "Sender oppdrag for fagsystem=${oppdrag.oppdrag110.kodeFagomraade} og " +
                    "fagsak=${oppdrag.oppdrag110.fagsystemId} behandling=$oppdragId til Oppdragsystemet"
        }

        try {
            // Create JMS objects
            val queue = MQQueue(getProperty("MQ_QUEUE"))
            queueConnection = createQueueConnection()
            queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE)
            queueSender = queueSession.createSender(queue)

            // Create a message
            val message = queueSession.createTextMessage(oppdragXml)
            message.jmsReplyTo = MQQueue(getProperty("MQ_MOTTAK")) // kvitteringsKø

            // Send the message
            queueSender.send(message)

        } catch (e: Exception) {
            defaultLogger.error { "Klarte ikke sende Oppdrag til OS. Feil: $e" }
            throw e
        } finally {
            if (::queueSender.isInitialized) {
                try {
                    queueSender.close()
                } catch (jmsex: JMSException) {
                    defaultLogger.warn { "queueSender could not be closed." }
                }
            }

            if (::queueSession.isInitialized) {
                try {
                    queueSession.close()
                } catch (jmsex: JMSException) {
                    defaultLogger.warn { "queueSession could not be closed." }
                }
            }

            if (::queueConnection.isInitialized) {
                try {
                    queueConnection.close()
                } catch (jmsex: JMSException) {
                    defaultLogger.warn { "queueConnection could not be closed." }
                }
            }
        }

        return oppdrag.oppdrag110.fagsystemId
    }
}
