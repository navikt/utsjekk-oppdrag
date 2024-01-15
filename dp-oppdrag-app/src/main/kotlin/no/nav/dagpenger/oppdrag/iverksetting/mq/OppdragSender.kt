package no.nav.dagpenger.oppdrag.iverksetting.mq

import com.ibm.mq.jakarta.jms.MQQueue
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.jms.JmsException
import org.springframework.jms.core.JmsTemplate
import org.springframework.stereotype.Service
import java.lang.UnsupportedOperationException

@Service
internal class OppdragSender(
    private val jmsTemplateUtgående: JmsTemplate,
    @Value("\${oppdrag.mq.enabled}") val erEnabled: String,
    @Value("\${oppdrag.mq.mottak}") val kvitteringsKø: String,
) {
    fun sendOppdrag(oppdrag: Oppdrag): String {
        if (!erEnabled.toBoolean()) {
            logger.info("MQ-integrasjon mot oppdrag er skrudd av")
            throw UnsupportedOperationException("Kan ikke sende melding til oppdrag. Integrasjonen er skrudd av.")
        }

        val oppdragId = oppdrag.oppdrag110?.oppdragsLinje150?.lastOrNull()?.henvisning
        val oppdragXml = OppdragXmlMapper.tilXml(oppdrag)

        logger.info(
            "Sender oppdrag for fagsystem=${oppdrag.oppdrag110.kodeFagomraade} og " +
                "fagsak=${oppdrag.oppdrag110.fagsystemId} behandling=$oppdragId til Oppdragsystemet",
        )

        try {
            jmsTemplateUtgående.send { session ->
                session.createTextMessage(oppdragXml).apply {
                    jmsReplyTo = MQQueue(kvitteringsKø)
                }
            }
        } catch (e: JmsException) {
            logger.error("Klarte ikke sende Oppdrag til OS. Feil: ", e)
            throw e
        }

        return oppdrag.oppdrag110.fagsystemId
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(OppdragSender::class.java)
    }
}
