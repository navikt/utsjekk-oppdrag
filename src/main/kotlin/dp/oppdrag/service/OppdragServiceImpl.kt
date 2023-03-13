package dp.oppdrag.service

import dp.oppdrag.model.OppdragId
import dp.oppdrag.model.OppdragLager
import dp.oppdrag.model.Utbetalingsoppdrag
import dp.oppdrag.repository.OppdragAlleredeSendtException
import dp.oppdrag.repository.OppdragLagerRepositoryJdbc
import dp.oppdrag.sender.OppdragSenderMQ
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.postgresql.util.PSQLException
import java.sql.SQLIntegrityConstraintViolationException
import javax.sql.DataSource

class OppdragServiceImpl(dataSource: DataSource) : OppdragService {

    private val oppdragLagerRepository = OppdragLagerRepositoryJdbc(dataSource)
    private val oppdragSender = OppdragSenderMQ()

    override fun opprettOppdrag(utbetalingsoppdrag: Utbetalingsoppdrag, oppdrag: Oppdrag, versjon: Int) {

        try {
            oppdragLagerRepository.opprettOppdrag(OppdragLager.lagFraOppdrag(utbetalingsoppdrag, oppdrag), versjon)
        } catch (exception: SQLIntegrityConstraintViolationException) { // H2
            throw OppdragAlleredeSendtException()
        } catch (exception: PSQLException) { // PostgreSQL
            if (exception.sqlState == "23505") {
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
