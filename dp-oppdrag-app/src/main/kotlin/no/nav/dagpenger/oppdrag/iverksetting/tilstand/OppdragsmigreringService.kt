package no.nav.dagpenger.oppdrag.iverksetting.tilstand

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
internal class OppdragsmigreringService(private val oppdragRepository: OppdragRepository) {
    @Scheduled(initialDelay = 120000, fixedDelay = 3600000)
    @Transactional
    fun migrer() {
        val oppdragUtenId = oppdragRepository.findWhereUuidIsNull()

        if (oppdragUtenId.none()) {
            logger.info("Migrering for uuid fullført.")
            return
        }

        oppdragUtenId.forEach { oppdragRepository.updateUuid(it.behandlingId, it.personIdent, it.fagsystem, it.versjon) }
        logger.info("Migrert  ${oppdragUtenId.count()} oppdraglager for uuid.")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}
