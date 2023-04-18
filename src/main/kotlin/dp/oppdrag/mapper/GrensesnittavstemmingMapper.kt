package dp.oppdrag.mapper

import dp.oppdrag.model.OppdragLager
import dp.oppdrag.model.OppdragLagerStatus
import dp.oppdrag.model.OppdragSkjemaConstants.Companion.FAGSYSTEM
import dp.oppdrag.model.OppdragSkjemaConstants.Companion.OPPDRAGSSYSTEMET
import dp.oppdrag.utils.encodeUUIDBase64
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class GrensesnittavstemmingMapper(
    private val oppdragsliste: List<OppdragLager>,
    private val fom: LocalDateTime,
    private val tom: LocalDateTime
) {
    private val antallDetaljerPerMelding = 70
    private val tidspunktFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS")
    val avstemmingId = encodeUUIDBase64(UUID.randomUUID())

    fun lagAvstemmingsmeldinger(): List<Avstemmingsdata> {
        return if (oppdragsliste.isEmpty())
            emptyList()
        else
            (listOf(lagStartmelding()) + lagDatameldinger() + listOf(lagSluttmelding()))
    }

    private fun lagStartmelding() = lagMelding(AksjonType.START)

    private fun lagSluttmelding() = lagMelding(AksjonType.AVSL)

    private fun lagDatameldinger(): List<Avstemmingsdata> {
        val detaljMeldinger = opprettAvstemmingsdataLister()
        val avstemmingsDataLister = detaljMeldinger.ifEmpty { listOf(lagMelding(AksjonType.DATA)) }

        avstemmingsDataLister.first().apply {
            this.total = opprettTotalData()
            this.periode = opprettPeriodeData()
            this.grunnlag = opprettGrunnlagsData()
        }

        return avstemmingsDataLister
    }

    private fun lagMelding(aksjonType: AksjonType): Avstemmingsdata =
        Avstemmingsdata().apply {
            aksjon = opprettAksjonsdata(aksjonType)
        }

    private fun opprettAksjonsdata(aksjonType: AksjonType): Aksjonsdata {
        return Aksjonsdata().apply {
            this.aksjonType = aksjonType
            this.kildeType = KildeType.AVLEV
            this.avstemmingType = AvstemmingType.GRSN
            this.avleverendeKomponentKode = FAGSYSTEM
            this.mottakendeKomponentKode = OPPDRAGSSYSTEMET
            this.underkomponentKode = FAGSYSTEM
            this.nokkelFom = fom.format(tidspunktFormatter)
            this.nokkelTom = tom.format(tidspunktFormatter)
            this.avleverendeAvstemmingId = avstemmingId
            this.brukerId = FAGSYSTEM
        }
    }

    private fun opprettAvstemmingsdataLister(): List<Avstemmingsdata> {
        return opprettDetaljdata().chunked(antallDetaljerPerMelding).map {
            lagMelding(AksjonType.DATA).apply {
                this.detalj.addAll(it)
            }
        }
    }

    private fun opprettDetaljdata(): List<Detaljdata> {
        return oppdragsliste.mapNotNull { oppdrag ->
            val detaljType = opprettDetaljType(oppdrag)
            if (detaljType != null) {
                val utbetalingsoppdrag = oppdrag.utbetalingsoppdrag
                Detaljdata().apply {
                    this.detaljType = detaljType
                    this.offnr = utbetalingsoppdrag.aktoer
                    this.avleverendeTransaksjonNokkel = FAGSYSTEM
                    this.tidspunkt = oppdrag.avstemmingTidspunkt.format(tidspunktFormatter)
                    if (detaljType in listOf(DetaljType.AVVI, DetaljType.VARS) && oppdrag.kvitteringsmelding != null) {
                        val kvitteringsmelding = oppdrag.kvitteringsmelding
                        this.meldingKode = kvitteringsmelding.kodeMelding
                        this.alvorlighetsgrad = kvitteringsmelding.alvorlighetsgrad
                        this.tekstMelding = kvitteringsmelding.beskrMelding
                    }
                }
            } else {
                null
            }
        }
    }

    private fun opprettDetaljType(oppdrag: OppdragLager): DetaljType? =
        when (oppdrag.status) {
            OppdragLagerStatus.LAGT_PAA_KOE -> DetaljType.MANG
            OppdragLagerStatus.KVITTERT_MED_MANGLER -> DetaljType.VARS
            OppdragLagerStatus.KVITTERT_FUNKSJONELL_FEIL -> DetaljType.AVVI
            OppdragLagerStatus.KVITTERT_TEKNISK_FEIL -> DetaljType.AVVI
            OppdragLagerStatus.KVITTERT_OK -> null
            OppdragLagerStatus.KVITTERT_UKJENT -> null
        }

    private fun opprettTotalData(): Totaldata {
        val totalBeloep = oppdragsliste.sumOf { getSatsBeloep(it) }
        return Totaldata().apply {
            this.totalAntall = oppdragsliste.size
            this.totalBelop = BigDecimal.valueOf(totalBeloep)
            this.fortegn = getFortegn(totalBeloep)
        }
    }

    private fun opprettPeriodeData(): Periodedata {
        return Periodedata().apply {
            this.datoAvstemtFom =
                formaterTilPeriodedataFormat(getLavesteAvstemmingstidspunkt().format(tidspunktFormatter))
            this.datoAvstemtTom =
                formaterTilPeriodedataFormat(getHoeyesteAvstemmingstidspunkt().format(tidspunktFormatter))
        }
    }

    private fun opprettGrunnlagsData(): Grunnlagsdata {
        var godkjentAntall = 0
        var godkjentBelop = 0L
        var varselAntall = 0
        var varselBelop = 0L
        var avvistAntall = 0
        var avvistBelop = 0L
        var manglerAntall = 0
        var manglerBelop = 0L

        for (oppdrag in oppdragsliste) {
            val satsbeloep = getSatsBeloep(oppdrag)
            when (oppdrag.status) {
                OppdragLagerStatus.LAGT_PAA_KOE -> {
                    manglerBelop += satsbeloep
                    manglerAntall++
                }

                OppdragLagerStatus.KVITTERT_OK -> {
                    godkjentBelop += satsbeloep
                    godkjentAntall++
                }

                OppdragLagerStatus.KVITTERT_MED_MANGLER -> {
                    varselBelop += satsbeloep
                    varselAntall++
                }

                else -> {
                    avvistBelop += satsbeloep
                    avvistAntall++
                }
            }
        }

        return Grunnlagsdata().apply {
            this.godkjentAntall = godkjentAntall
            this.godkjentBelop = BigDecimal.valueOf(godkjentBelop)
            this.godkjentFortegn = getFortegn(godkjentBelop)

            this.varselAntall = varselAntall
            this.varselBelop = BigDecimal.valueOf(varselBelop)
            this.varselFortegn = getFortegn(varselBelop)

            this.manglerAntall = manglerAntall
            this.manglerBelop = BigDecimal.valueOf(manglerBelop)
            this.manglerFortegn = getFortegn(manglerBelop)

            this.avvistAntall = avvistAntall
            this.avvistBelop = BigDecimal.valueOf(avvistBelop)
            this.avvistFortegn = getFortegn(avvistBelop)
        }
    }

    private fun getSatsBeloep(oppdrag: OppdragLager): Long =
        oppdrag.utbetalingsoppdrag.utbetalingsperiode.map { it.sats }.reduce(BigDecimal::add).toLong()

    private fun getFortegn(satsbeloep: Long): Fortegn {
        return if (satsbeloep >= 0) Fortegn.T else Fortegn.F
    }

    private fun getHoeyesteAvstemmingstidspunkt(): LocalDateTime {
        return sortertAvstemmingstidspunkt().first()
    }

    private fun getLavesteAvstemmingstidspunkt(): LocalDateTime {
        return sortertAvstemmingstidspunkt().last()
    }

    private fun sortertAvstemmingstidspunkt() =
        oppdragsliste.map(OppdragLager::avstemmingTidspunkt).sortedDescending()

    private fun formaterTilPeriodedataFormat(stringTimestamp: String): String =
        LocalDateTime.parse(stringTimestamp, tidspunktFormatter)
            .format(DateTimeFormatter.ofPattern("yyyyMMddHH"))
}
