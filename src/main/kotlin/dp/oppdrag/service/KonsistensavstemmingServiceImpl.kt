package dp.oppdrag.service

import dp.oppdrag.defaultLogger
import dp.oppdrag.mapper.KonsistensavstemmingMapper
import dp.oppdrag.model.KonsistensavstemmingRequest
import dp.oppdrag.model.OppdragSkjemaConstants.Companion.FAGSYSTEM
import dp.oppdrag.model.Utbetalingsoppdrag
import dp.oppdrag.model.UtbetalingsoppdragForKonsistensavstemming
import dp.oppdrag.repository.MellomlagringKonsistensavstemmingRepository
import dp.oppdrag.repository.OppdragLagerRepository
import dp.oppdrag.sender.AvstemmingSenderMQ
import java.time.LocalDateTime
import java.util.*

class KonsistensavstemmingServiceImpl(
    private val oppdragLagerRepository: OppdragLagerRepository,
    mellomlagringKonsistensavstemmingRepository: MellomlagringKonsistensavstemmingRepository
) :
    KonsistensavstemmingService {

    private val mellomlagringKonsistensavstemmingService = MellomlagringKonsistensavstemmingServiceImpl(
        mellomlagringKonsistensavstemmingRepository
    )
    private val avstemmingSender = AvstemmingSenderMQ()

    override fun utfoerKonsistensavstemming(
        request: KonsistensavstemmingRequest,
        sendStartMelding: Boolean,
        sendAvsluttmelding: Boolean,
        transaksjonsId: UUID?
    ) {
        sjekkAtTransaktionsIdErSattHvisSplittetBatch(sendStartMelding, sendAvsluttmelding, transaksjonsId)

        val avstemmingstidspunkt = request.avstemmingstidspunkt
        val perioderPaaBehandling = request.perioderForBehandlinger.associate { it.behandlingId to it.perioder }
        verifyUnikeBehandlinger(perioderPaaBehandling, request)

        val fnrPaaBehandling = request.perioderForBehandlinger.associate { it.behandlingId to it.aktivFoedselsnummer }

        val utbetalingsoppdragForKonsistensavstemming =
            oppdragLagerRepository.hentUtbetalingsoppdragForKonsistensavstemming(FAGSYSTEM, perioderPaaBehandling.keys)

        val utbetalingsoppdrag = leggAktuellePerioderISisteUtbetalingsoppdraget(
            utbetalingsoppdragForKonsistensavstemming,
            perioderPaaBehandling,
            fnrPaaBehandling
        )

        utfoerKonsistensavstemming(
            KonsistensavstemmingMetaInfo(
                fagsystem = FAGSYSTEM,
                transaksjonsId = transaksjonsId,
                avstemmingstidspunkt = avstemmingstidspunkt,
                sendStartmelding = sendStartMelding,
                sendAvsluttmelding = sendAvsluttmelding,
                utbetalingsoppdrag = utbetalingsoppdrag
            )
        )
    }

    private fun sjekkAtTransaktionsIdErSattHvisSplittetBatch(
        sendStartMelding: Boolean,
        sendAvsluttmelding: Boolean,
        transaksjonsId: UUID?
    ) {
        if (!(sendStartMelding && sendAvsluttmelding) && Objects.isNull(transaksjonsId)) {
            throw Exception("Er sendStartmelding eller sendAvsluttmelding satt til false må transaksjonsId være definert.")
        }
    }

    private fun verifyUnikeBehandlinger(
        periodeIderPaaBehandling: Map<String, Set<Long>>,
        request: KonsistensavstemmingRequest
    ) {
        if (periodeIderPaaBehandling.size != request.perioderForBehandlinger.size) {
            val duplikateBehandlinger = request.perioderForBehandlinger
                .map { it.behandlingId }
                .groupingBy { it }
                .eachCount()
                .filter { it.value > 1 }
            error("Behandling finnes flere ganger i requesten: ${duplikateBehandlinger.keys}")
        }
    }

    private fun leggAktuellePerioderISisteUtbetalingsoppdraget(
        utbetalingsoppdrag: List<UtbetalingsoppdragForKonsistensavstemming>,
        perioderPaaBehandling: Map<String, Set<Long>>,
        foedselsnummerPaaBehandling: Map<String, String>
    ): List<Utbetalingsoppdrag> {
        val utbetalingsoppdragPaaFagsak = utbetalingsoppdrag.groupBy { it.fagsakId }

        return utbetalingsoppdragPaaFagsak.map { (saksnummer, utbetalingsoppdragListe) ->
            val senesteUtbetalingsoppdrag = utbetalingsoppdragListe.maxByOrNull { oppdrag ->
                oppdrag.utbetalingsoppdrag.utbetalingsperiode.maxOf { it.periodeId }
            }?.utbetalingsoppdrag ?: error("Finner ikke seneste behandling for fagsak=$saksnummer")

            val behandlingsIderForFagsak = utbetalingsoppdragListe.map { it.behandlingId }.toSet()

            val aktuellePeriodeIderForFagsak =
                perioderPaaBehandling.filter { behandlingsIderForFagsak.contains(it.key) }.values.flatten().toSet()

            var aktivtFoedselsnummer: String? = null
            val perioderTilKonsistensavstemming = utbetalingsoppdragListe.flatMap {
                aktivtFoedselsnummer = hentFnrForBehandling(foedselsnummerPaaBehandling, it.behandlingId)
                it.utbetalingsoppdrag.utbetalingsperiode
                    .filter { utbetalingsperiode -> aktuellePeriodeIderForFagsak.contains(utbetalingsperiode.periodeId) }
                    // Setter aktivt fødselsnummer på behandling som mottok fra fagsystem
                    .map { utbetalingsperiode ->
                        utbetalingsperiode.copy(utbetalesTil = aktivtFoedselsnummer ?: utbetalingsperiode.utbetalesTil)
                    }
            }

            senesteUtbetalingsoppdrag.let {
                it.copy(
                    utbetalingsperiode = perioderTilKonsistensavstemming,
                    // Setter aktivt fødselsnummer på behandling som mottok fra fagsystem
                    aktoer = aktivtFoedselsnummer ?: it.aktoer
                )
            }
        }
    }

    private fun hentFnrForBehandling(fnrPaaBehandling: Map<String, String>, behandlingId: String): String {
        return fnrPaaBehandling[behandlingId]
            ?: error("Finnes ikke et aktivt fødselsnummer for behandlingId $behandlingId")
    }

    private fun utfoerKonsistensavstemming(
        metaInfo: KonsistensavstemmingMetaInfo
    ) {
        if (metaInfo.erFoersteBatchIEnSplittetBatch()) {
            mellomlagringKonsistensavstemmingService.sjekkAtDetteErFoersteMelding(metaInfo.transaksjonsId!!)
        }

        val konsistensavstemmingMapper = opprettKonsistensavstemmingMapper(metaInfo)

        val meldinger = konsistensavstemmingMapper.lagAvstemmingsmeldinger()

        if (meldinger.isEmpty()) {
            defaultLogger.info { "Ingen oppdrag å utføre konsistensavstemming for" }
            return
        }

        defaultLogger.info {
            "Utfører konsistensavstemming for id ${konsistensavstemmingMapper.avstemmingId} " +
                    "antall meldinger er ${meldinger.size}"
        }

        meldinger.forEach {
            avstemmingSender.sendKonsistensAvstemming(it)
        }

        if (metaInfo.erSplittetBatchMenIkkeSisteBatch()) {
            mellomlagringKonsistensavstemmingService.opprettInnslagIMellomlagring(
                metaInfo,
                konsistensavstemmingMapper.antallOppdrag,
                konsistensavstemmingMapper.totalBeloep
            )
        }
        defaultLogger.info { "Fullført konsistensavstemming for id ${konsistensavstemmingMapper.avstemmingId}" }
    }

    private fun opprettKonsistensavstemmingMapper(
        metaInfo: KonsistensavstemmingMetaInfo,
    ): KonsistensavstemmingMapper {
        val aggregertAntallOppdrag = mellomlagringKonsistensavstemmingService.hentAggregertAntallOppdrag(metaInfo)
        val aggregertTotalBeloep = mellomlagringKonsistensavstemmingService.hentAggregertBeloep(metaInfo)

        return KonsistensavstemmingMapper(
            fagsystem = metaInfo.fagsystem,
            utbetalingsoppdrag = metaInfo.utbetalingsoppdrag,
            avstemmingsDato = metaInfo.avstemmingstidspunkt,
            sendStartmelding = metaInfo.sendStartmelding,
            sendAvsluttmelding = metaInfo.sendAvsluttmelding,
            aggregertAntallOppdrag = aggregertAntallOppdrag,
            aggregertTotalBeloep = aggregertTotalBeloep,
            transaksjonsId = metaInfo.transaksjonsId,
        )
    }
}

data class KonsistensavstemmingMetaInfo(
    val fagsystem: String,
    val transaksjonsId: UUID?,
    val avstemmingstidspunkt: LocalDateTime,
    val sendStartmelding: Boolean,
    val sendAvsluttmelding: Boolean,
    val utbetalingsoppdrag: List<Utbetalingsoppdrag>,
) {

    fun erFoersteBatchIEnSplittetBatch(): Boolean = sendStartmelding && !sendAvsluttmelding
    fun erSisteBatchIEnSplittetBatch(): Boolean = !sendStartmelding && sendAvsluttmelding
    fun erSplittetBatchMenIkkeSisteBatch(): Boolean = erSplittetBatch() && !erSisteBatchIEnSplittetBatch()
    fun erSplittetBatch(): Boolean = !sendStartmelding || !sendAvsluttmelding
}
