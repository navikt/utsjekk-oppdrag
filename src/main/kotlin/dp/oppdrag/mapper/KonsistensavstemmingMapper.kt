package dp.oppdrag.mapper

import dp.oppdrag.defaultLogger
import dp.oppdrag.model.OppdragSkjemaConstants.Companion.BRUK_KJOEREPLAN_DEFAULT
import dp.oppdrag.model.OppdragSkjemaConstants.Companion.ENHET
import dp.oppdrag.model.OppdragSkjemaConstants.Companion.ENHET_DATO_FOM
import dp.oppdrag.model.OppdragSkjemaConstants.Companion.ENHET_TYPE
import dp.oppdrag.model.OppdragSkjemaConstants.Companion.FAGSYSTEM
import dp.oppdrag.model.OppdragSkjemaConstants.Companion.FRADRAG_TILLEGG
import dp.oppdrag.model.OppdragSkjemaConstants.Companion.OPPDRAGSSYSTEMET
import dp.oppdrag.model.OppdragSkjemaConstants.Companion.OPPDRAG_GJELDER_DATO_FOM
import dp.oppdrag.model.SatsTypeKode
import dp.oppdrag.model.UtbetalingsfrekvensKode
import dp.oppdrag.model.Utbetalingsoppdrag
import dp.oppdrag.model.Utbetalingsperiode
import dp.oppdrag.utils.encodeUUIDBase64
import no.nav.virksomhet.tjenester.avstemming.informasjon.konsistensavstemmingsdata.v1.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class KonsistensavstemmingMapper(
    private val fagsystem: String,
    private val utbetalingsoppdrag: List<Utbetalingsoppdrag>,
    private val avstemmingsDato: LocalDateTime,
    private var aggregertTotalBeloep: Long,
    private var aggregertAntallOppdrag: Int,
    private val sendStartmelding: Boolean,
    private val sendAvsluttmelding: Boolean,
    transaksjonsId: UUID? = UUID.randomUUID()
) {

    private val START = "START"
    private val AVSLUTT = "AVSL"
    private val DATA = "DATA"
    private val KILDETYPE = "AVLEV"
    private val KONSISTENSAVSTEMMING = "KONS"
    private val FORTEGN_T = "T"
    private val FORTEGN_F = "F"
    private val tidspunktFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS")
    private val datoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    val avstemmingId = encodeUUIDBase64(transaksjonsId ?: UUID.randomUUID())
    var totalBeloep = 0L
    var antallOppdrag = 0
    private val behandledeSaker = mutableSetOf<String>()

    fun lagAvstemmingsmeldinger(): List<Konsistensavstemmingsdata> =
        when {
            sendStartmelding && sendAvsluttmelding -> (
                    listOf(lagStartmelding()) + lagDatameldinger() + listOf(
                        lagTotaldata(),
                        lagSluttmelding()
                    )
                    )

            sendStartmelding -> (listOf(lagStartmelding()) + lagDatameldinger())
            sendAvsluttmelding -> (lagDatameldinger() + listOf(lagTotaldata(), lagSluttmelding()))
            else -> lagDatameldinger()
        }

    private fun lagStartmelding() = lagAksjonsmelding(START)

    private fun lagSluttmelding() = lagAksjonsmelding(AVSLUTT)

    private fun lagDatameldinger(): List<Konsistensavstemmingsdata> {
        val dataListe: MutableList<Konsistensavstemmingsdata> = arrayListOf()

        for (utbetalingsoppdrag in utbetalingsoppdrag) {
            if (!behandledeSaker.add(utbetalingsoppdrag.saksnummer))
                error("Har allerede lagt til ${utbetalingsoppdrag.saksnummer} i listen over avstemminger")

            val konsistensavstemmingsdata = lagAksjonsmelding(DATA)
            konsistensavstemmingsdata.apply {
                oppdragsdataListe.add(lagOppdragsdata(utbetalingsoppdrag))
            }
            dataListe.add(konsistensavstemmingsdata)
        }
        antallOppdrag = dataListe.size
        return dataListe
    }

    private fun lagOppdragsdata(utbetalingsoppdrag: Utbetalingsoppdrag): Oppdragsdata {
        return Oppdragsdata().apply {
            fagomradeKode = utbetalingsoppdrag.fagSystem
            fagsystemId = utbetalingsoppdrag.saksnummer
            utbetalingsfrekvens = UtbetalingsfrekvensKode.MAANEDLIG.kode
            oppdragGjelderId = utbetalingsoppdrag.aktoer
            oppdragGjelderFom = OPPDRAG_GJELDER_DATO_FOM.format(datoFormatter)
            saksbehandlerId = utbetalingsoppdrag.saksbehandlerId
            oppdragsenhetListe.add(lagEnhet())
            utbetalingsoppdrag.utbetalingsperiode
                .filter { verifiserAtPerioderErAktiv(it) }
                .map { periode ->
                    oppdragslinjeListe.add(
                        lagOppdragsLinjeListe(
                            utbetalingsperiode = periode,
                            utbetalingsoppdrag = utbetalingsoppdrag
                        )
                    )
                }
        }
    }

    private fun lagOppdragsLinjeListe(
        utbetalingsperiode: Utbetalingsperiode,
        utbetalingsoppdrag: Utbetalingsoppdrag
    ): Oppdragslinje {
        totalBeloep += utbetalingsperiode.sats.toLong()
        return Oppdragslinje().apply {
            vedtakId = utbetalingsperiode.datoForVedtak.format(datoFormatter)
            delytelseId = utbetalingsoppdrag.saksnummer + utbetalingsperiode.periodeId
            utbetalingsperiode.forrigePeriodeId?.let {
                refDelytelseId = utbetalingsoppdrag.saksnummer + it
            }
            klassifikasjonKode = utbetalingsperiode.klassifisering
            vedtakPeriode = Periode().apply {
                fom = utbetalingsperiode.vedtakdatoFom.format(datoFormatter)
                tom = utbetalingsperiode.vedtakdatoTom.format(datoFormatter)
            }
            sats = utbetalingsperiode.sats
            satstypeKode = SatsTypeKode.fromKode(utbetalingsperiode.satsType.name).kode
            brukKjoreplan = BRUK_KJOEREPLAN_DEFAULT
            fradragTillegg = FRADRAG_TILLEGG.value()
            saksbehandlerId = utbetalingsoppdrag.saksbehandlerId
            utbetalesTilId = utbetalingsperiode.utbetalesTil
            henvisning = utbetalingsperiode.behandlingId
            attestantListe.add(lagAttestant(utbetalingsoppdrag))

            utbetalingsperiode.utbetalingsgrad?.let { utbetalingsgrad ->
                gradListe.add(
                    Grad().apply {
                        gradKode = "UBGR"
                        grad = utbetalingsgrad
                    }
                )
            }
        }
    }

    private fun verifiserAtPerioderErAktiv(utbetalingsperiode: Utbetalingsperiode): Boolean {
        val avstemmingsdato = avstemmingsDato.toLocalDate()
        val vedtakdatoTom = utbetalingsperiode.vedtakdatoTom
        val aktiv = !vedtakdatoTom.isBefore(avstemmingsdato)
        if (!aktiv) {
            defaultLogger.error {
                "fagsystem=$fagsystem vedtakdatoTom=$vedtakdatoTom (periodens tom-dato) " +
                        "er etter avstemmingsdato=$avstemmingsdato for" +
                        " periodeId=${utbetalingsperiode.periodeId} behandlingId=${utbetalingsperiode.behandlingId}"
            }
        }
        return aktiv
    }

    private fun lagAttestant(utbetalingsoppdrag: Utbetalingsoppdrag): Attestant {
        return Attestant().apply {
            attestantId = utbetalingsoppdrag.saksbehandlerId
        }
    }

    private fun lagEnhet(): Enhet {
        return Enhet().apply {
            enhetType = ENHET_TYPE
            enhet = ENHET
            enhetFom = ENHET_DATO_FOM.format(datoFormatter)
        }
    }

    private fun lagTotaldata(): Konsistensavstemmingsdata {
        val konsistensavstemmingsdata = lagAksjonsmelding(DATA)
        konsistensavstemmingsdata.apply {
            totaldata = Totaldata().apply {
                totalAntall = antallOppdrag.toBigInteger() + aggregertAntallOppdrag.toBigInteger()
                totalBelop = BigDecimal.valueOf(totalBeloep) + BigDecimal.valueOf(aggregertTotalBeloep)
                fortegn = getFortegn(totalBeloep + aggregertTotalBeloep)
            }
        }
        return konsistensavstemmingsdata
    }

    private fun getFortegn(satsbeloep: Long): String {
        return if (BigDecimal.valueOf(satsbeloep) >= BigDecimal.ZERO)
            FORTEGN_T else FORTEGN_F
    }

    private fun lagAksjonsmelding(aksjontype: String): Konsistensavstemmingsdata =
        Konsistensavstemmingsdata().apply {
            aksjonsdata = opprettAksjonsdata(aksjontype)
        }

    private fun opprettAksjonsdata(aksjonstype: String): Aksjonsdata {
        return Aksjonsdata().apply {
            this.aksjonsType = aksjonstype
            this.kildeType = KILDETYPE
            this.avstemmingType = KONSISTENSAVSTEMMING
            this.avleverendeKomponentKode = FAGSYSTEM
            this.mottakendeKomponentKode = OPPDRAGSSYSTEMET
            this.underkomponentKode = fagsystem
            this.tidspunktAvstemmingTom = avstemmingsDato.format(tidspunktFormatter)
            this.avleverendeAvstemmingId = avstemmingId
            this.brukerId = fagsystem
        }
    }

}