package dp.oppdrag.service

import dp.oppdrag.defaultLogger
import dp.oppdrag.mapper.GrensesnittavstemmingMapper
import dp.oppdrag.model.GrensesnittavstemmingRequest
import dp.oppdrag.model.OppdragSkjemaConstants.Companion.FAGSYSTEM
import dp.oppdrag.repository.OppdragLagerRepository
import dp.oppdrag.sender.AvstemmingSenderMQ
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Grunnlagsdata
import java.time.LocalDateTime

class GrensesnittavstemmingServiceImpl(private val oppdragLagerRepository: OppdragLagerRepository) :
    GrensesnittavstemmingService {

    private val packageName = "dp.oppdrag.grensesnittavstemming"
    private val countere: Map<String, Counter> = opprettMetrikkerForFagsystem()
    private val avstemmingSender = AvstemmingSenderMQ()

    override fun utfoerGrensesnittavstemming(request: GrensesnittavstemmingRequest) {
        val (fra: LocalDateTime, til: LocalDateTime) = request
        val oppdragSomSkalAvstemmes = oppdragLagerRepository.hentIverksettingerForGrensesnittavstemming(fra, til)
        val avstemmingMapper = GrensesnittavstemmingMapper(oppdragSomSkalAvstemmes, fra, til)
        val meldinger = avstemmingMapper.lagAvstemmingsmeldinger()

        if (meldinger.isEmpty()) {
            defaultLogger.info { "Ingen oppdrag å gjennomføre grensesnittavstemming for." }
            return
        }

        defaultLogger.info { "Utfører grensesnittavstemming for id: ${avstemmingMapper.avstemmingId}, ${meldinger.size} antall meldinger." }

        meldinger.forEach {
            avstemmingSender.sendGrensesnittAvstemming(it)
        }

        defaultLogger.info { "Fullført grensesnittavstemming for id: ${avstemmingMapper.avstemmingId}" }

        oppdaterMetrikker(meldinger[1].grunnlag)
    }

    private fun oppdaterMetrikker(grunnlag: Grunnlagsdata) {
        countere.getValue(Status.GODKJENT.status).increment(grunnlag.godkjentAntall.toDouble())
        countere.getValue(Status.AVVIST.status).increment(grunnlag.avvistAntall.toDouble())
        countere.getValue(Status.MANGLER.status).increment(grunnlag.manglerAntall.toDouble())
        countere.getValue(Status.VARSEL.status).increment(grunnlag.varselAntall.toDouble())
    }

    private fun opprettMetrikkerForFagsystem(): Map<String, Counter> {
        val godkjentCounter = Metrics.counter(
            packageName,
            "fagsystem", FAGSYSTEM,
            "status", Status.GODKJENT.status,
            "beskrivelse", Status.GODKJENT.beskrivelse
        )
        val avvistCounter = Metrics.counter(
            packageName,
            "fagsystem", FAGSYSTEM,
            "status", Status.AVVIST.status,
            "beskrivelse", Status.AVVIST.beskrivelse
        )
        val manglerCounter = Metrics.counter(
            packageName,
            "fagsystem", FAGSYSTEM,
            "status", Status.MANGLER.status,
            "beskrivelse", Status.MANGLER.beskrivelse
        )
        val varselCounter = Metrics.counter(
            packageName,
            "fagsystem", FAGSYSTEM,
            "status", Status.VARSEL.status,
            "beskrivelse", Status.VARSEL.beskrivelse
        )

        return hashMapOf(
            Status.GODKJENT.status to godkjentCounter,
            Status.AVVIST.status to avvistCounter,
            Status.MANGLER.status to manglerCounter,
            Status.VARSEL.status to varselCounter
        )
    }
}

enum class Status(val status: String, val beskrivelse: String) {
    GODKJENT(
        "godkjent",
        "Antall oppdrag som har fått OK kvittering (alvorlighetsgrad 00)."
    ),
    AVVIST(
        "avvist",
        "Antall oppdrag som har fått kvittering med funksjonell eller teknisk feil, samt ukjent (alvorlighetsgrad 08 og 12)."
    ),
    MANGLER(
        "mangler",
        "Antall oppdrag hvor kvittering mangler."
    ),
    VARSEL(
        "varsel",
        "Antall oppdrag som har fått kvittering med mangler (alvorlighetsgrad 04)."
    )
}
