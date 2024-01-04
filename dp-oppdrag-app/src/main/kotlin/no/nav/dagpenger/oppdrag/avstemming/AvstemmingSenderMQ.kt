package no.nav.dagpenger.oppdrag.avstemming

import no.nav.dagpenger.oppdrag.grensesnittavstemming.JaxbGrensesnittAvstemmingsdata
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Avstemmingsdata
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.jms.JmsException
import org.springframework.jms.core.JmsTemplate
import org.springframework.stereotype.Service

@Service
class AvstemmingSenderMQ(
    val jmsTemplateAvstemming: JmsTemplate,
    @Value("\${oppdrag.mq.enabled}") val erEnabled: String
) : AvstemmingSender {

    override fun sendGrensesnittAvstemming(avstemmingsdata: Avstemmingsdata) {

        val avstemmingXml = JaxbGrensesnittAvstemmingsdata.tilXml(avstemmingsdata)
        leggPåKø(avstemmingXml)
    }

    private fun leggPåKø(melding: String) {
        if (!erEnabled.toBoolean()) {
            LOG.info("MQ-integrasjon mot oppdrag er skrudd av. Kan ikke sende avstemming")
            throw UnsupportedOperationException("Kan ikke sende avstemming til oppdrag. Integrasjonen er skrudd av.")
        }

        try {
            jmsTemplateAvstemming.convertAndSend(
                "queue:///${jmsTemplateAvstemming.defaultDestinationName}?targetClient=1",
                melding
            )
        } catch (e: JmsException) {
            LOG.error("Klarte ikke sende avstemming til OS. Feil: ", e)
            throw e
        }
    }

    companion object {
        val LOG: Logger = LoggerFactory.getLogger(AvstemmingSenderMQ::class.java)
    }
}
