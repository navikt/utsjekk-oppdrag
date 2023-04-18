package dp.oppdrag.sender

import com.ibm.mq.jms.MQQueue
import dp.oppdrag.defaultLogger
import dp.oppdrag.defaultXmlMapper
import dp.oppdrag.utils.createQueueConnection
import dp.oppdrag.utils.getProperty
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Avstemmingsdata
import javax.jms.*

class AvstemmingSenderMQ : AvstemmingSender {

    private lateinit var queueConnection: QueueConnection
    private lateinit var queueSession: QueueSession
    private lateinit var queueSender: QueueSender

    override fun sendGrensesnittAvstemming(avstemmingsdata: Avstemmingsdata) {
        if (!getProperty("MQ_ENABLED").toBoolean()) {
            defaultLogger.info { "MQ-integrasjon mot oppdrag er skrudd av. Kan ikke sende avstemming" }
            throw UnsupportedOperationException("Kan ikke sende avstemming til oppdrag. Integrasjonen er skrudd av.")
        }

        try {
            // Create JMS objects
            val queue = MQQueue(getProperty("MQ_AVSTEMMING_QUEUE"))
            queueConnection = createQueueConnection()
            queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE)
            queueSender = queueSession.createSender(queue)

            // Create a message
            val avstemmingsdataXml = defaultXmlMapper.writeValueAsString(avstemmingsdata)
            val message = queueSession.createTextMessage(avstemmingsdataXml)

            // Send the message
            queueSender.send(message)

        } catch (e: Exception) {
            defaultLogger.error { "Klarte ikke sende Avstemming til OS. Feil: $e" }
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
    }
}
