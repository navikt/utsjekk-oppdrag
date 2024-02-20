package no.nav.dagpenger.oppdrag.iverksetting.mq

import jakarta.jms.TextMessage
import no.nav.dagpenger.kontrakter.oppdrag.OppdragStatus
import no.nav.dagpenger.oppdrag.iverksetting.domene.kvitteringstatus
import no.nav.dagpenger.oppdrag.iverksetting.domene.status
import no.nav.dagpenger.oppdrag.iverksetting.tilstand.OppdragLagerRepository
import no.nav.dagpenger.oppdrag.iverksetting.tilstand.dekomprimertId
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
internal class OppdragMottaker(
    private val oppdragLagerRepository: OppdragLagerRepository,
    private val env: Environment,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(OppdragMottaker::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }

    private val kjørerLokalt get() = env.activeProfiles.any { it in setOf("local") }

    @Transactional
    @JmsListener(destination = "\${oppdrag.mq.mottak}", containerFactory = "jmsListenerContainerFactory")
    fun mottaKvitteringFraOppdrag(melding: TextMessage) {
        try {
            behandleMelding(melding)
        } catch (e: Exception) {
            secureLogger.warn("Feilet lesing av melding=${melding.jmsMessageID}", e)
            throw e
        }
    }

    private fun behandleMelding(melding: TextMessage) {
        val kvittering = lesKvittering(parseTextMessage(melding))
        val oppdragId = kvittering.dekomprimertId

        logger.info(
            "Mottatt melding på kvitteringskø for fagsak $oppdragId: Status ${kvittering.kvitteringstatus}, " +
                "svar ${kvittering.mmel?.beskrMelding ?: "Beskrivende melding ikke satt fra OS"}",
        )
        logger.debug("Henter oppdrag $oppdragId fra databasen")

        val førsteOppdragUtenKvittering =
            oppdragLagerRepository.hentAlleVersjonerAvOppdrag(oppdragId)
                .find { it.status == OppdragStatus.LAGT_PÅ_KØ }

        if (førsteOppdragUtenKvittering == null) {
            logger.warn("Oppdraget tilknyttet mottatt kvittering har uventet status i databasen. Oppdraget er: $oppdragId")
            return
        }

        if (kvittering.mmel != null) {
            oppdragLagerRepository.oppdaterKvitteringsmelding(oppdragId, kvittering.mmel, førsteOppdragUtenKvittering.versjon)
        }

        if (!kjørerLokalt) {
            logger.debug("Lagrer oppdatert oppdrag $oppdragId i databasen med ny status ${kvittering.status}")
            oppdragLagerRepository.oppdaterStatus(oppdragId, kvittering.status, førsteOppdragUtenKvittering.versjon)
        } else {
            oppdragLagerRepository.oppdaterStatus(oppdragId, OppdragStatus.KVITTERT_OK, førsteOppdragUtenKvittering.versjon)
        }
    }

    private fun parseTextMessage(melding: TextMessage) =
        melding.text.let { text ->
            if (!kjørerLokalt) {
                text.replace("oppdrag xmlns", "ns2:oppdrag xmlns:ns2")
            } else {
                text
            }
        }

    fun lesKvittering(melding: String) = OppdragXmlMapper.tilOppdrag(melding)
}
