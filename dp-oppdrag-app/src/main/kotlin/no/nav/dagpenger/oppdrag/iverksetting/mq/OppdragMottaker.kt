package no.nav.dagpenger.oppdrag.iverksetting.mq

import jakarta.jms.TextMessage
import no.nav.dagpenger.kontrakter.oppdrag.OppdragStatus
import no.nav.dagpenger.oppdrag.iverksetting.domene.kvitteringstatus
import no.nav.dagpenger.oppdrag.iverksetting.domene.status
import no.nav.dagpenger.oppdrag.iverksetting.tilstand.OppdragLagerRepository
import no.nav.dagpenger.oppdrag.iverksetting.tilstand.dekomprimertId
import no.nav.dagpenger.oppdrag.iverksetting.tilstand.id
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
            logger.warn("Feilet lesing av melding med id: ${melding.jmsMessageID}, innhold: ${melding.text}", e)
            throw e
        }
    }

    private fun behandleMelding(melding: TextMessage) {
        val kvittering = OppdragXmlMapper.tilOppdrag(melding.text)
        val oppdragIdKvittering = kvittering.dekomprimertId

        logger.info(
            "Mottatt melding på kvitteringskø for fagsak $oppdragIdKvittering: Status ${kvittering.kvitteringstatus}, " +
                "svar ${kvittering.mmel?.beskrMelding ?: "Beskrivende melding ikke satt fra OS"}",
        )
        logger.debug("Henter oppdrag {} fra databasen", oppdragIdKvittering)

        val førsteOppdragUtenKvittering =
            oppdragLagerRepository.hentAlleVersjonerAvOppdrag(oppdragIdKvittering)
                .find { it.status == OppdragStatus.LAGT_PÅ_KØ }

        if (førsteOppdragUtenKvittering == null) {
            logger.warn("Oppdraget tilknyttet mottatt kvittering har uventet status i databasen. Oppdraget er: $oppdragIdKvittering")
            return
        }
        val oppdragId = førsteOppdragUtenKvittering.id

        if (kvittering.mmel != null) {
            oppdragLagerRepository.oppdaterKvitteringsmelding(oppdragId, kvittering.mmel, førsteOppdragUtenKvittering.versjon)
        }

        if (!kjørerLokalt) {
            logger.debug("Lagrer oppdatert oppdrag {} i databasen med ny status {}", oppdragId, kvittering.status)
            oppdragLagerRepository.oppdaterStatus(oppdragId, kvittering.status, førsteOppdragUtenKvittering.versjon)
        } else {
            oppdragLagerRepository.oppdaterStatus(oppdragId, OppdragStatus.KVITTERT_OK, førsteOppdragUtenKvittering.versjon)
        }
    }
}
