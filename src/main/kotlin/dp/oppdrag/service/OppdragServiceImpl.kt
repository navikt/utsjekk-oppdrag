package dp.oppdrag.service

import dp.oppdrag.model.OppdragId
import dp.oppdrag.model.OppdragLager
import dp.oppdrag.model.Utbetalingsoppdrag
import dp.oppdrag.repository.OppdragLagerRepositoryJdbc
import dp.oppdrag.sender.OppdragSenderMQ
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import javax.sql.DataSource

class OppdragServiceImpl(dataSource: DataSource) : OppdragService {

    private val oppdragLagerRepository = OppdragLagerRepositoryJdbc(dataSource)
    private val oppdragSender = OppdragSenderMQ()

    override fun opprettOppdrag(utbetalingsoppdrag: Utbetalingsoppdrag, oppdrag: Oppdrag, versjon: Int) {

        oppdragLagerRepository.opprettOppdrag(OppdragLager.lagFraOppdrag(utbetalingsoppdrag, oppdrag), versjon)
        oppdragSender.sendOppdrag(oppdrag)
    }

    override fun hentStatusForOppdrag(oppdragId: OppdragId): OppdragLager {
        return oppdragLagerRepository.hentOppdrag(oppdragId)
    }
}
