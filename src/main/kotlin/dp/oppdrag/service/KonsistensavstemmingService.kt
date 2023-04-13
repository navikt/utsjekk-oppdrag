package dp.oppdrag.service

import dp.oppdrag.model.KonsistensavstemmingRequest
import java.util.*

interface KonsistensavstemmingService {

    fun utfoerKonsistensavstemming(
        request: KonsistensavstemmingRequest,
        sendStartMelding: Boolean,
        sendAvsluttmelding: Boolean,
        transaksjonsId: UUID?
    )
}
