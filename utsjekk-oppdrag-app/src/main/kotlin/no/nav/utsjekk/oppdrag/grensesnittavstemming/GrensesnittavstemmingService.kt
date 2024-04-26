package no.nav.utsjekk.oppdrag.grensesnittavstemming

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.utsjekk.kontrakter.felles.Fagsystem
import no.nav.utsjekk.oppdrag.iverksetting.tilstand.OppdragLagerRepository
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Grunnlagsdata
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.EnumMap

@Service
internal class GrensesnittavstemmingService(
    private val avstemmingSender: GrensesnittavstemmingSender,
    private val oppdragLagerRepository: OppdragLagerRepository,
) {
    private val tellere: MutableMap<Fagsystem, Map<String, Counter>> = EnumMap(Fagsystem::class.java)

    init {
        enumValues<Fagsystem>().forEach {
            tellere[it] = metrikkerForFagsystem(it)
        }
    }

    fun utførGrensesnittavstemming(
        fagsystem: Fagsystem,
        fra: LocalDateTime,
        til: LocalDateTime,
    ) {
        val oppdragSomSkalAvstemmes =
            oppdragLagerRepository.hentIverksettingerForGrensesnittavstemming(
                fomTidspunkt = fra,
                tomTidspunkt = til,
                fagsystem = fagsystem,
            )
        val avstemmingMapper = GrensesnittavstemmingMapper(oppdragSomSkalAvstemmes, fagsystem, fra, til)
        val meldinger = avstemmingMapper.lagAvstemmingsmeldinger()

        if (oppdragSomSkalAvstemmes.isEmpty()) {
            logger.info("Ingen oppdrag å gjennomføre grensesnittavstemming for.")
            return
        }

        logger.info("Utfører grensesnittavstemming med id ${avstemmingMapper.avstemmingId} for fagsystem $fagsystem, " +
                "${meldinger.size} antall meldinger, ${oppdragSomSkalAvstemmes.size} antall oppdrag.")

        meldinger.forEach {
            avstemmingSender.sendGrensesnittAvstemming(it)
        }

        logger.info("Fullført grensesnittavstemming for id: ${avstemmingMapper.avstemmingId}")

        oppdaterMetrikker(fagsystem, meldinger[1].grunnlag)
    }

    private fun oppdaterMetrikker(
        fagsystem: Fagsystem,
        grunnlag: Grunnlagsdata,
    ) {
        val metrikkerForFagsystem = tellere.getValue(fagsystem)

        metrikkerForFagsystem.getValue(Status.GODKJENT.status).increment(grunnlag.godkjentAntall.toDouble())
        metrikkerForFagsystem.getValue(Status.AVVIST.status).increment(grunnlag.avvistAntall.toDouble())
        metrikkerForFagsystem.getValue(Status.MANGLER.status).increment(grunnlag.manglerAntall.toDouble())
        metrikkerForFagsystem.getValue(Status.VARSEL.status).increment(grunnlag.varselAntall.toDouble())
    }

    private fun metrikkerForFagsystem(fagsystem: Fagsystem) =
        hashMapOf(
            Status.GODKJENT.status to tellerForFagsystem(fagsystem, Status.GODKJENT),
            Status.AVVIST.status to tellerForFagsystem(fagsystem, Status.AVVIST),
            Status.MANGLER.status to tellerForFagsystem(fagsystem, Status.MANGLER),
            Status.VARSEL.status to tellerForFagsystem(fagsystem, Status.VARSEL),
        )

    private fun tellerForFagsystem(
        fagsystem: Fagsystem,
        status: Status,
    ) = Metrics.counter(
        "utsjekk.oppdrag.grensesnittavstemming",
        "fagsystem",
        fagsystem.name,
        "status",
        status.status,
        "beskrivelse",
        status.beskrivelse,
    )

    companion object {
        val logger: Logger = LoggerFactory.getLogger(GrensesnittavstemmingService::class.java)
    }
}

enum class Status(val status: String, val beskrivelse: String) {
    GODKJENT("godkjent", "Antall oppdrag som har fått OK kvittering (alvorlighetsgrad 00)."),
    AVVIST(
        "avvist",
        "Antall oppdrag som har fått kvittering med funksjonell eller teknisk feil, samt ukjent (alvorlighetsgrad 08 og 12).",
    ),
    MANGLER("mangler", "Antall oppdrag hvor kvittering mangler."),
    VARSEL("varsel", "Antall oppdrag som har fått kvittering med mangler (alvorlighetsgrad 04)."),
}
