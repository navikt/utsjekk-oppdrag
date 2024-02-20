package no.nav.dagpenger.oppdrag.iverksetting

import no.nav.dagpenger.kontrakter.oppdrag.Utbetalingsoppdrag
import no.nav.dagpenger.oppdrag.iverksetting.mq.OppdragSender
import no.nav.dagpenger.oppdrag.iverksetting.tilstand.OppdragId
import no.nav.dagpenger.oppdrag.iverksetting.tilstand.OppdragLager
import no.nav.dagpenger.oppdrag.iverksetting.tilstand.OppdragLagerRepository
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
internal class OppdragService(
    @Autowired private val oppdragSender: OppdragSender,
    @Autowired private val oppdragLagerRepository: OppdragLagerRepository,
) {
    @Transactional(rollbackFor = [Throwable::class])
    fun opprettOppdrag(
        utbetalingsoppdrag: Utbetalingsoppdrag,
        oppdrag: Oppdrag,
        versjon: Int,
    ) {
        try {
            logger.debug("Lagrer oppdrag med saksnummer ${utbetalingsoppdrag.saksnummer} i databasen")
            oppdragLagerRepository.opprettOppdrag(OppdragLager.lagFraOppdrag(utbetalingsoppdrag, oppdrag), versjon)
        } catch (e: org.springframework.dao.DuplicateKeyException) {
            logger.info("Oppdrag med saksnummer ${utbetalingsoppdrag.saksnummer} er allerede sendt.")
            throw OppdragAlleredeSendtException()
        }

        logger.debug("Legger oppdrag med saksnummer ${utbetalingsoppdrag.saksnummer} på kø")
        oppdragSender.sendOppdrag(oppdrag)
    }

    fun hentStatusForOppdrag(oppdragId: OppdragId) = oppdragLagerRepository.hentOppdrag(oppdragId)

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(OppdragService::class.java)
    }
}

internal class OppdragAlleredeSendtException : RuntimeException()
