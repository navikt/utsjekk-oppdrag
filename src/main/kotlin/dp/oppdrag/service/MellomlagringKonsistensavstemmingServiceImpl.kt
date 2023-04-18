package dp.oppdrag.service

import dp.oppdrag.defaultLogger
import dp.oppdrag.model.MellomlagringKonsistensavstemming
import dp.oppdrag.repository.MellomlagringKonsistensavstemmingRepository
import java.util.*

class MellomlagringKonsistensavstemmingServiceImpl(
    private val mellomlagringKonsistensavstemmingRepository: MellomlagringKonsistensavstemmingRepository
) : MellomlagringKonsistensavstemmingService {

    override fun hentAggregertBeloep(
        metaInfo: KonsistensavstemmingMetaInfo
    ): Long =
        if (metaInfo.erSisteBatchIEnSplittetBatch()) {
            mellomlagringKonsistensavstemmingRepository.hentAggregertTotalBeloep(metaInfo.transaksjonsId!!)
        } else {
            0L
        }

    override fun hentAggregertAntallOppdrag(
        metaInfo: KonsistensavstemmingMetaInfo
    ): Int {
        return if (metaInfo.erSisteBatchIEnSplittetBatch()) {
            mellomlagringKonsistensavstemmingRepository.hentAggregertAntallOppdrag(metaInfo.transaksjonsId!!)
        } else {
            0
        }
    }

    override fun opprettInnslagIMellomlagring(
        metaInfo: KonsistensavstemmingMetaInfo,
        antalOppdrag: Int,
        totalBeloep: Long,
    ) {
        val mellomlagring = MellomlagringKonsistensavstemming(
            fagsystem = metaInfo.fagsystem,
            transaksjonsId = metaInfo.transaksjonsId!!,
            antallOppdrag = antalOppdrag,
            totalBeloep = totalBeloep,
        )
        mellomlagringKonsistensavstemmingRepository.insert(mellomlagring)
        defaultLogger.info { "Opprettet mellomlagring for transaksjonsId ${metaInfo.transaksjonsId}" }
    }

    override fun sjekkAtDetteErFoersteMelding(transaksjonsId: UUID) {
        if (mellomlagringKonsistensavstemmingRepository.findAllByTransaksjonsId(transaksjonsId).isNotEmpty()) {
            throw Exception("Skal sende startmelding men det er ikke første mottatte batch med transaskjsonId= $transaksjonsId")
        }
    }
}
