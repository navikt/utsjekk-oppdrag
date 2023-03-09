package dp.oppdrag.service

import dp.oppdrag.model.OppdragId
import dp.oppdrag.model.OppdragLager
import dp.oppdrag.model.Utbetalingsoppdrag
import dp.oppdrag.repository.OppdragAlleredeSendtException
import dp.oppdrag.repository.OppdragLagerRepositoryJdbc
import dp.oppdrag.sender.OppdragSenderMQ
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import java.sql.SQLIntegrityConstraintViolationException
import javax.sql.DataSource

class OppdragServiceImpl(dataSource: DataSource) : OppdragService {

    private val oppdragLagerRepository = OppdragLagerRepositoryJdbc(dataSource)
    private val oppdragSender = OppdragSenderMQ()

    override fun opprettOppdrag(utbetalingsoppdrag: Utbetalingsoppdrag, oppdrag: Oppdrag, versjon: Int) {

        try {
            oppdragLagerRepository.opprettOppdrag(OppdragLager.lagFraOppdrag(utbetalingsoppdrag, oppdrag), versjon)
        } catch (e: SQLIntegrityConstraintViolationException) {
            throw OppdragAlleredeSendtException()
        }

        // oppdragSender.sendOppdrag(oppdrag)
    }

    override fun hentStatusForOppdrag(oppdragId: OppdragId): OppdragLager {
        return oppdragLagerRepository.hentOppdrag(oppdragId)
    }
}
