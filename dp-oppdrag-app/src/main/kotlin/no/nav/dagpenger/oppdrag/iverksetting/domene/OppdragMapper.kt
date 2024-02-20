package no.nav.dagpenger.oppdrag.iverksetting.domene

import no.nav.dagpenger.kontrakter.felles.GeneriskIdSomString
import no.nav.dagpenger.kontrakter.felles.GeneriskIdSomUUID
import no.nav.dagpenger.kontrakter.felles.somString
import no.nav.dagpenger.kontrakter.felles.somUUID
import no.nav.dagpenger.kontrakter.oppdrag.OppdragStatus
import no.nav.dagpenger.kontrakter.oppdrag.Utbetalingsoppdrag
import no.nav.dagpenger.kontrakter.oppdrag.Utbetalingsperiode
import no.nav.dagpenger.oppdrag.iverksetting.domene.UuidKomprimator.komprimer
import no.trygdeetaten.skjema.oppdrag.ObjectFactory
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import no.trygdeetaten.skjema.oppdrag.Oppdrag110
import no.trygdeetaten.skjema.oppdrag.OppdragsLinje150
import no.trygdeetaten.skjema.oppdrag.TkodeStatusLinje
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.GregorianCalendar
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar

internal object OppdragMapper {
    private val objectFactory = ObjectFactory()

    val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS")

    fun tilOppdrag110(utbetalingsoppdrag: Utbetalingsoppdrag): Oppdrag110 =
        objectFactory.createOppdrag110().apply {
            kodeAksjon = OppdragSkjemaConstants.KODE_AKSJON
            kodeEndring = Endringskode.fromKode(utbetalingsoppdrag.kodeEndring.name).kode
            kodeFagomraade = utbetalingsoppdrag.fagsystem.kode
            fagsystemId = utbetalingsoppdrag.komprimertFagsystemId
            utbetFrekvens = Utbetalingsfrekvens.MÅNEDLIG.kode
            oppdragGjelderId = utbetalingsoppdrag.aktør
            datoOppdragGjelderFom = OppdragSkjemaConstants.OPPDRAG_GJELDER_DATO_FOM.toXMLDate()
            saksbehId = utbetalingsoppdrag.saksbehandlerId
            avstemming115 =
                objectFactory.createAvstemming115().apply {
                    nokkelAvstemming = utbetalingsoppdrag.avstemmingstidspunkt.format(timeFormatter)
                    kodeKomponent = utbetalingsoppdrag.fagsystem.kode
                    tidspktMelding = utbetalingsoppdrag.avstemmingstidspunkt.format(timeFormatter)
                }
            tilOppdragsEnhet120(utbetalingsoppdrag).map { oppdragsEnhet120.add(it) }
            utbetalingsoppdrag.utbetalingsperiode.map { periode ->
                oppdragsLinje150.add(
                    tilOppdragsLinje150(
                        utbetalingsperiode = periode,
                        utbetalingsoppdrag = utbetalingsoppdrag,
                    ),
                )
            }
        }

    private fun tilOppdragsEnhet120(utbetalingsoppdrag: Utbetalingsoppdrag) =
        if (utbetalingsoppdrag.brukersNavKontor == null) {
            listOf(
                objectFactory.createOppdragsEnhet120().apply {
                    enhet = OppdragSkjemaConstants.ENHET
                    typeEnhet = OppdragSkjemaConstants.ENHET_TYPE_BOSTEDSENHET
                    datoEnhetFom = OppdragSkjemaConstants.ENHET_DATO_FOM.toXMLDate()
                },
            )
        } else {
            listOf(
                objectFactory.createOppdragsEnhet120().apply {
                    enhet = utbetalingsoppdrag.brukersNavKontor?.enhet
                    typeEnhet = OppdragSkjemaConstants.ENHET_TYPE_BOSTEDSENHET
                    datoEnhetFom = utbetalingsoppdrag.brukersNavKontor?.gjelderFom?.toXMLDate()
                },
                objectFactory.createOppdragsEnhet120().apply {
                    enhet = OppdragSkjemaConstants.ENHET
                    typeEnhet = OppdragSkjemaConstants.ENHET_TYPE_BEHANDLENDE_ENHET
                    datoEnhetFom = OppdragSkjemaConstants.ENHET_DATO_FOM.toXMLDate()
                },
            )
        }

    private fun tilOppdragsLinje150(
        utbetalingsperiode: Utbetalingsperiode,
        utbetalingsoppdrag: Utbetalingsoppdrag,
    ): OppdragsLinje150 {
        val sakIdKomprimert = utbetalingsoppdrag.komprimertFagsystemId

        val attestant =
            objectFactory.createAttestant180().apply {
                attestantId = utbetalingsoppdrag.saksbehandlerId
            }

        return objectFactory.createOppdragsLinje150().apply {
            kodeEndringLinje =
                if (utbetalingsperiode.erEndringPåEksisterendePeriode) Endringskode.ENDRING.kode else Endringskode.NY.kode
            utbetalingsperiode.opphør?.let {
                kodeStatusLinje = TkodeStatusLinje.OPPH
                datoStatusFom = it.fom.toXMLDate()
            }
            if (!utbetalingsperiode.erEndringPåEksisterendePeriode) {
                utbetalingsperiode.forrigePeriodeId?.let {
                    refDelytelseId = "$sakIdKomprimert#$it"
                    refFagsystemId = sakIdKomprimert
                }
            }
            vedtakId = utbetalingsperiode.vedtaksdato.toString()
            delytelseId = "$sakIdKomprimert#${utbetalingsperiode.periodeId}"
            kodeKlassifik = utbetalingsperiode.klassifisering
            datoVedtakFom = utbetalingsperiode.fom.toXMLDate()
            datoVedtakTom = utbetalingsperiode.tom.toXMLDate()
            sats = utbetalingsperiode.sats
            fradragTillegg = OppdragSkjemaConstants.FRADRAG_TILLEGG
            typeSats = utbetalingsperiode.satstype.tilOppdragskode()
            brukKjoreplan = OppdragSkjemaConstants.BRUK_KJØREPLAN_DEFAULT
            saksbehId = utbetalingsoppdrag.saksbehandlerId
            utbetalesTilId = utbetalingsperiode.utbetalesTil
            henvisning =
                when (utbetalingsperiode.behandlingId) {
                    is GeneriskIdSomString -> utbetalingsperiode.behandlingId.somString
                    is GeneriskIdSomUUID -> utbetalingsperiode.behandlingId.somUUID.komprimer()
                }
            attestant180.add(attestant)

            utbetalingsperiode.utbetalingsgrad?.let { utbetalingsgrad ->
                grad170.add(
                    objectFactory.createGrad170().apply {
                        typeGrad = Gradtype.UTBETALINGSGRAD.kode
                        grad = utbetalingsgrad.toBigInteger()
                    },
                )
            }
        }
    }

    fun tilOppdrag(oppdrag110: Oppdrag110): Oppdrag =
        objectFactory.createOppdrag().apply {
            this.oppdrag110 = oppdrag110
        }
}

internal val Utbetalingsoppdrag.fagsystemId get() = this.saksnummer.somString

internal val Utbetalingsoppdrag.komprimertFagsystemId get(): String {
    return when (this.saksnummer) {
        is GeneriskIdSomString -> this.saksnummer.somString
        is GeneriskIdSomUUID -> this.saksnummer.somUUID.komprimer()
    }
}

internal val Oppdrag.status: OppdragStatus
    get() =
        when (Kvitteringstatus.fraKode(mmel?.alvorlighetsgrad ?: "Ukjent")) {
            Kvitteringstatus.OK -> OppdragStatus.KVITTERT_OK
            Kvitteringstatus.AKSEPTERT_MEN_NOE_ER_FEIL -> OppdragStatus.KVITTERT_MED_MANGLER
            Kvitteringstatus.AVVIST_FUNKSJONELLE_FEIL -> OppdragStatus.KVITTERT_FUNKSJONELL_FEIL
            Kvitteringstatus.AVVIST_TEKNISK_FEIL -> OppdragStatus.KVITTERT_TEKNISK_FEIL
            Kvitteringstatus.UKJENT -> OppdragStatus.KVITTERT_UKJENT
        }

internal fun LocalDate.toXMLDate(): XMLGregorianCalendar =
    DatatypeFactory.newInstance()
        .newXMLGregorianCalendar(GregorianCalendar.from(atStartOfDay(ZoneId.systemDefault())))
