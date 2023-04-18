package dp.oppdrag.repository

import dp.oppdrag.model.MellomlagringKonsistensavstemming
import java.util.*

interface MellomlagringKonsistensavstemmingRepository {
    fun findAllByTransaksjonsId(
        transaksjonsId: UUID
    ): List<MellomlagringKonsistensavstemming>

    fun hentAggregertAntallOppdrag(transaksjonsId: UUID): Int

    fun hentAggregertTotalBeloep(transaksjonsId: UUID): Long

    fun insert(mellomlagringKonsistensavstemming: MellomlagringKonsistensavstemming)
}
