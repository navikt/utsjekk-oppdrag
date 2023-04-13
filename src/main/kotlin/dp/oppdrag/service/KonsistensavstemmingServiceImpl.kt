package dp.oppdrag.service

import dp.oppdrag.model.KonsistensavstemmingRequest
import dp.oppdrag.repository.OppdragLagerRepository
import java.util.*

class KonsistensavstemmingServiceImpl(private val oppdragLagerRepository: OppdragLagerRepository) :
    KonsistensavstemmingService {

    override fun utfoerKonsistensavstemming(
        request: KonsistensavstemmingRequest,
        sendStartMelding: Boolean,
        sendAvsluttmelding: Boolean,
        transaksjonsId: UUID?
    ) {

    }
}
