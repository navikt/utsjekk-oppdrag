package no.nav.dagpenger.oppdrag.service

import no.nav.dagpenger.kontrakter.oppdrag.OppdragId
import no.nav.dagpenger.kontrakter.oppdrag.Utbetalingsoppdrag
import no.nav.dagpenger.oppdrag.domene.OppdragStatus
import no.nav.dagpenger.oppdrag.domene.id
import no.nav.dagpenger.oppdrag.repository.OppdragLager
import no.nav.dagpenger.oppdrag.repository.OppdragLagerRepository
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Profile("e2e")
class OppdragServiceE2E(
    @Autowired private val oppdragLagerRepository: OppdragLagerRepository
) : OppdragService {

    @Transactional(rollbackFor = [Throwable::class])
    override fun opprettOppdrag(utbetalingsoppdrag: Utbetalingsoppdrag, oppdrag: Oppdrag, versjon: Int) {
        LOG.debug("Lagrer oppdrag i databasen " + oppdrag.id)
        oppdragLagerRepository.opprettOppdrag(OppdragLager.lagFraOppdrag(utbetalingsoppdrag, oppdrag), versjon)

        LOG.debug("Kvittering mottat ok " + oppdrag.id)
        oppdragLagerRepository.oppdaterStatus(oppdrag.id, OppdragStatus.KVITTERT_OK)
    }

    override fun hentStatusForOppdrag(oppdragId: OppdragId): OppdragLager {
        return oppdragLagerRepository.hentOppdrag(oppdragId)
    }

    companion object {

        val LOG: Logger = LoggerFactory.getLogger(OppdragServiceE2E::class.java)
    }
}
