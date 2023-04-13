package dp.oppdrag.service

import dp.oppdrag.model.GrensesnittavstemmingRequest

interface GrensesnittavstemmingService {

    fun utfoerGrensesnittavstemming(request: GrensesnittavstemmingRequest)
}
