package dp.oppdrag.service

import dp.oppdrag.model.OppdragId
import dp.oppdrag.model.OppdragLager
import dp.oppdrag.model.Utbetalingsoppdrag
import dp.oppdrag.repository.OppdragAlleredeSendtException
import dp.oppdrag.repository.OppdragLagerRepository
import dp.oppdrag.sender.OppdragSenderMQ
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.postgresql.util.PSQLException

class OppdragServiceImpl(private val oppdragLagerRepository: OppdragLagerRepository) : OppdragService {

    private val oppdragSender = OppdragSenderMQ()

    override fun opprettOppdrag(utbetalingsoppdrag: Utbetalingsoppdrag, oppdrag: Oppdrag, versjon: Int) {

        try {
            oppdragLagerRepository.opprettOppdrag(OppdragLager.lagFraOppdrag(utbetalingsoppdrag, oppdrag), versjon)
        } catch (exception: PSQLException) {
            if (exception.sqlState == "23505") { //  Integrity Constraint Violation (Unique Violation)
                throw OppdragAlleredeSendtException()
            } else {
                throw exception
            }
        }

        oppdragSender.sendOppdrag(oppdrag)
    }

    override fun hentStatusForOppdrag(oppdragId: OppdragId): OppdragLager {
        return oppdragLagerRepository.hentOppdrag(oppdragId)
    }
}
