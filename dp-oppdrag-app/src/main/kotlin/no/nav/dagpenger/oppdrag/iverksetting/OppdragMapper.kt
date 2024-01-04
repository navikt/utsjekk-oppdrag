package no.nav.dagpenger.oppdrag.iverksetting

import no.nav.dagpenger.kontrakter.felles.SakIdentifikator
import no.nav.dagpenger.kontrakter.oppdrag.Utbetalingsoppdrag
import no.nav.dagpenger.kontrakter.oppdrag.Utbetalingsperiode
import no.nav.dagpenger.oppdrag.iverksetting.UuidUtils.komprimer
import no.trygdeetaten.skjema.oppdrag.ObjectFactory
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import no.trygdeetaten.skjema.oppdrag.Oppdrag110
import no.trygdeetaten.skjema.oppdrag.OppdragsEnhet120
import no.trygdeetaten.skjema.oppdrag.OppdragsLinje150
import no.trygdeetaten.skjema.oppdrag.TkodeStatusLinje
import org.springframework.stereotype.Component
import java.time.format.DateTimeFormatter

@Component
class OppdragMapper {

    private val objectFactory = ObjectFactory()
    val tidspunktFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS")

    fun tilOppdrag110(utbetalingsoppdrag: Utbetalingsoppdrag): Oppdrag110 {

        val avstemming = objectFactory.createAvstemming115().apply {
            nokkelAvstemming = utbetalingsoppdrag.avstemmingTidspunkt.format(tidspunktFormatter)
            kodeKomponent = utbetalingsoppdrag.fagSystem.kode
            tidspktMelding = utbetalingsoppdrag.avstemmingTidspunkt.format(tidspunktFormatter)
        }

        val oppdrag110 = objectFactory.createOppdrag110().apply {
            kodeAksjon = OppdragSkjemaConstants.KODE_AKSJON
            kodeEndring = EndringsKode.fromKode(utbetalingsoppdrag.kodeEndring.name).kode
            kodeFagomraade = utbetalingsoppdrag.fagSystem.kode
            fagsystemId = utbetalingsoppdrag.tilFagsystemId()
            utbetFrekvens = UtbetalingsfrekvensKode.MÅNEDLIG.kode
            oppdragGjelderId = utbetalingsoppdrag.aktoer
            datoOppdragGjelderFom = OppdragSkjemaConstants.OPPDRAG_GJELDER_DATO_FOM.toXMLDate()
            saksbehId = utbetalingsoppdrag.saksbehandlerId
            avstemming115 = avstemming
            tilOppdragsEnhet120(utbetalingsoppdrag).map { oppdragsEnhet120.add(it) }
            utbetalingsoppdrag.utbetalingsperiode.map { periode ->
                oppdragsLinje150.add(tilOppdragsLinje150(utbetalingsperiode = periode, utbetalingsoppdrag = utbetalingsoppdrag))
            }
        }

        return oppdrag110
    }

    private fun tilOppdragsEnhet120(utbetalingsoppdrag: Utbetalingsoppdrag): List<OppdragsEnhet120> {
        return if (utbetalingsoppdrag.brukersNavKontor == null) {
            listOf(
                objectFactory.createOppdragsEnhet120().apply {
                    enhet = OppdragSkjemaConstants.ENHET
                    typeEnhet = OppdragSkjemaConstants.ENHET_TYPE_BOSTEDSENHET
                    datoEnhetFom = OppdragSkjemaConstants.ENHET_DATO_FOM.toXMLDate()
                }
            )
        } else {
            listOf(
                objectFactory.createOppdragsEnhet120().apply {
                    enhet = OppdragSkjemaConstants.ENHET
                    typeEnhet = OppdragSkjemaConstants.ENHET_TYPE_BEHANDLENDE_ENHET
                    datoEnhetFom = OppdragSkjemaConstants.ENHET_DATO_FOM.toXMLDate()
                },
                objectFactory.createOppdragsEnhet120().apply {
                    enhet = utbetalingsoppdrag.brukersNavKontor?.enhet
                    typeEnhet = OppdragSkjemaConstants.ENHET_TYPE_BOSTEDSENHET
                    datoEnhetFom = utbetalingsoppdrag.brukersNavKontor?.gjelderFom?.toXMLDate()
                }
            )
        }
    }

    private fun tilOppdragsLinje150(
        utbetalingsperiode: Utbetalingsperiode,
        utbetalingsoppdrag: Utbetalingsoppdrag
    ): OppdragsLinje150 {
        val sakIdKomprimert = utbetalingsoppdrag.tilFagsystemId()

        val attestant = objectFactory.createAttestant180().apply {
            attestantId = utbetalingsoppdrag.saksbehandlerId
        }

        return objectFactory.createOppdragsLinje150().apply {
            kodeEndringLinje =
                if (utbetalingsperiode.erEndringPåEksisterendePeriode) EndringsKode.ENDRING.kode else EndringsKode.NY.kode
            utbetalingsperiode.opphør?.let {
                kodeStatusLinje = TkodeStatusLinje.OPPH
                datoStatusFom = it.opphørDatoFom.toXMLDate()
            }
            if (!utbetalingsperiode.erEndringPåEksisterendePeriode) {
                utbetalingsperiode.forrigePeriodeId?.let {
                    refDelytelseId = "$sakIdKomprimert#$it"
                    refFagsystemId = sakIdKomprimert
                }
            }
            vedtakId = utbetalingsperiode.datoForVedtak.toString()
            delytelseId = "$sakIdKomprimert#${utbetalingsperiode.periodeId}"
            kodeKlassifik = utbetalingsperiode.klassifisering
            datoVedtakFom = utbetalingsperiode.vedtakdatoFom.toXMLDate()
            datoVedtakTom = utbetalingsperiode.vedtakdatoTom.toXMLDate()
            sats = utbetalingsperiode.sats
            fradragTillegg = OppdragSkjemaConstants.FRADRAG_TILLEGG
            typeSats = SatsTypeKode.fromKode(utbetalingsperiode.satsType.name).kode
            brukKjoreplan = OppdragSkjemaConstants.BRUK_KJØREPLAN_DEFAULT
            saksbehId = utbetalingsoppdrag.saksbehandlerId
            utbetalesTilId = utbetalingsperiode.utbetalesTil
            henvisning = utbetalingsperiode.behandlingId.komprimer()
            attestant180.add(attestant)

            utbetalingsperiode.utbetalingsgrad?.let { utbetalingsgrad ->
                grad170.add(
                    objectFactory.createGrad170().apply {
                        typeGrad = GradTypeKode.UTBETALINGSGRAD.kode
                        grad = utbetalingsgrad.toBigInteger()
                    }
                )
            }
        }
    }

    fun tilOppdrag(oppdrag110: Oppdrag110): Oppdrag {
        return objectFactory.createOppdrag().apply {
            this.oppdrag110 = oppdrag110
        }
    }
}

fun Utbetalingsoppdrag.tilFagsystemId(): String {
    SakIdentifikator.valider(this.saksnummer, this.saksreferanse)
    return this.saksnummer?.komprimer() ?: this.saksreferanse!!
}
