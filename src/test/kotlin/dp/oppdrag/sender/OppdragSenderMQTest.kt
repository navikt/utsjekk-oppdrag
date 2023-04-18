package dp.oppdrag.sender

import dp.oppdrag.defaultXmlMapper
import dp.oppdrag.model.OppdragSkjemaConstants.Companion.FAGSYSTEM
import dp.oppdrag.utils.createQueueConnection
import io.mockk.*
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import no.trygdeetaten.skjema.oppdrag.Oppdrag110
import no.trygdeetaten.skjema.oppdrag.OppdragsLinje150
import javax.jms.QueueConnection
import javax.jms.QueueSender
import javax.jms.QueueSession
import javax.jms.TextMessage
import kotlin.test.Test

class OppdragSenderMQTest {

    @Test
    fun shouldSendMessage() {
        // Override environmental variables
        System.setProperty("MQ_ENABLED", "true")
        System.setProperty("MQ_OPPDRAG_QUEUE", "TestQueue1")
        System.setProperty("MQ_KVITTERING_QUEUE", "TestQeue2")

        // Mock
        val queueSender = mockk<QueueSender>()
        every { queueSender.send(any()) } just Runs
        every { queueSender.close() } just Runs

        val textMessage = mockk<TextMessage>()
        every { textMessage.jmsReplyTo = any() } just Runs

        val queueSession = mockk<QueueSession>()
        every { queueSession.createSender(any()) } returns queueSender
        every { queueSession.createTextMessage(any()) } returns textMessage
        every { queueSession.close() } just Runs

        val queueConnection = mockk<QueueConnection>()
        every { queueConnection.createQueueSession(false, 1) } returns queueSession
        every { queueConnection.close() } just Runs

        mockkStatic(::createQueueConnection)
        every { createQueueConnection() } returns queueConnection

        // Testdata
        val oppdrag = Oppdrag()
            .withOppdrag110(
                Oppdrag110()
                    .withKodeFagomraade("KodeFagomraade")
                    .withOppdragGjelderId("123456789")
                    .withOppdragsLinje150(
                        OppdragsLinje150().withHenvisning("Henvisning")
                    )
                    .withFagsystemId(FAGSYSTEM)
            )

        // Run
        val oppdragSenderMQ = OppdragSenderMQ()
        oppdragSenderMQ.sendOppdrag(oppdrag)

        // Check
        verify { queueSession.createTextMessage(defaultXmlMapper.writeValueAsString(oppdrag)) }
        verify { queueSender.send(textMessage) }
    }
}
