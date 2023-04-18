package dp.oppdrag.service

import java.util.*

interface MellomlagringKonsistensavstemmingService {

    fun hentAggregertBeloep(metaInfo: KonsistensavstemmingMetaInfo): Long
    fun hentAggregertAntallOppdrag(metaInfo: KonsistensavstemmingMetaInfo): Int

    fun opprettInnslagIMellomlagring(metaInfo: KonsistensavstemmingMetaInfo, antalOppdrag: Int, totalBeloep: Long)

    fun sjekkAtDetteErFoersteMelding(transaksjonsId: UUID)
}
