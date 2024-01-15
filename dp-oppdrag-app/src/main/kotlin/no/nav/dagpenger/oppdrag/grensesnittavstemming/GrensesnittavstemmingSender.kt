package no.nav.dagpenger.oppdrag.grensesnittavstemming

import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Avstemmingsdata
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.jms.JmsException
import org.springframework.jms.core.JmsTemplate
import org.springframework.stereotype.Service

@Service
internal class GrensesnittavstemmingSender(
    val jmsTemplateAvstemming: JmsTemplate,
    @Value("\${oppdrag.mq.enabled}") val erEnabled: String,
) {
    fun sendGrensesnittAvstemming(avstemmingsdata: Avstemmingsdata) {
        if (!erEnabled.toBoolean()) {
            logger.info("MQ-integrasjon mot oppdrag er skrudd av. Kan ikke sende avstemming")
            throw UnsupportedOperationException("Kan ikke sende avstemming til oppdrag. Integrasjonen er skrudd av.")
        }

        try {
            val destination = "queue:///${jmsTemplateAvstemming.defaultDestinationName}?targetClient=1"
            val melding = GrensesnittavstemmingXmlMapper.tilXml(avstemmingsdata)

            jmsTemplateAvstemming.convertAndSend(destination, melding)
        } catch (e: JmsException) {
            logger.error("Klarte ikke sende avstemming til OS. Feil: ", e)
            throw e
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(GrensesnittavstemmingSender::class.java)
    }
}
