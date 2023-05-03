package dp.oppdrag.mapper

import dp.oppdrag.model.*
import dp.oppdrag.model.OppdragSkjemaConstants.Companion.BRUK_KJOEREPLAN_DEFAULT
import dp.oppdrag.model.OppdragSkjemaConstants.Companion.BRUK_KJOEREPLAN_G_OMBEREGNING
import dp.oppdrag.model.OppdragSkjemaConstants.Companion.ENHET
import dp.oppdrag.model.OppdragSkjemaConstants.Companion.ENHET_DATO_FOM
import dp.oppdrag.model.OppdragSkjemaConstants.Companion.ENHET_TYPE
import dp.oppdrag.model.OppdragSkjemaConstants.Companion.FRADRAG_TILLEGG
import dp.oppdrag.model.OppdragSkjemaConstants.Companion.KODE_AKSJON
import dp.oppdrag.model.OppdragSkjemaConstants.Companion.OPPDRAG_GJELDER_DATO_FOM
import dp.oppdrag.utils.toXMLDate
import no.trygdeetaten.skjema.oppdrag.*
import java.time.format.DateTimeFormatter

class OppdragMapper {

    private val objectFactory = ObjectFactory()
    private val tidspunktFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS")

    fun tilOppdrag(utbetalingsoppdrag: Utbetalingsoppdrag): Oppdrag {
        val oppdrag110 = tilOppdrag110(utbetalingsoppdrag)

        return objectFactory.createOppdrag().apply {
            this.oppdrag110 = oppdrag110
        }
    }

    private fun tilOppdrag110(utbetalingsoppdrag: Utbetalingsoppdrag): Oppdrag110 {

        val avstemming = objectFactory.createAvstemming115().apply {
            nokkelAvstemming = utbetalingsoppdrag.avstemmingTidspunkt.format(tidspunktFormatter)
            kodeKomponent = utbetalingsoppdrag.fagSystem
            tidspktMelding = utbetalingsoppdrag.avstemmingTidspunkt.format(tidspunktFormatter)
        }

        val oppdragsEnhet = objectFactory.createOppdragsEnhet120().apply {
            enhet = ENHET
            typeEnhet = ENHET_TYPE
            datoEnhetFom = ENHET_DATO_FOM.toXMLDate()
        }

        val oppdrag110 = objectFactory.createOppdrag110().apply {
            kodeAksjon = KODE_AKSJON
            kodeEndring = EndringsKode.fromKode(utbetalingsoppdrag.kodeEndring.name).kode
            kodeFagomraade = utbetalingsoppdrag.fagSystem
            fagsystemId = utbetalingsoppdrag.saksnummer
            utbetFrekvens = UtbetalingsfrekvensKode.MAANEDLIG.kode
            oppdragGjelderId = utbetalingsoppdrag.aktoer
            datoOppdragGjelderFom = OPPDRAG_GJELDER_DATO_FOM.toXMLDate()
            saksbehId = utbetalingsoppdrag.saksbehandlerId
            avstemming115 = avstemming
            oppdragsEnhet120.add(oppdragsEnhet)
            utbetalingsoppdrag.utbetalingsperiode.map { periode ->
                oppdragsLinje150.add(
                    tilOppdragsLinje150(
                        utbetalingsperiode = periode,
                        utbetalingsoppdrag = utbetalingsoppdrag
                    )
                )
            }
        }

        return oppdrag110
    }

    private fun tilOppdragsLinje150(
        utbetalingsperiode: Utbetalingsperiode,
        utbetalingsoppdrag: Utbetalingsoppdrag
    ): OppdragsLinje150 {

        return objectFactory.createOppdragsLinje150().apply {
            kodeEndringLinje =
                if (utbetalingsperiode.erEndringPaaEksisterendePeriode) EndringsKode.ENDRING.kode else EndringsKode.NY.kode
            utbetalingsperiode.opphoer?.let {
                kodeStatusLinje = TkodeStatusLinje.OPPH
                datoStatusFom = it.opphoerDatoFom.toXMLDate()
            }
            if (!utbetalingsperiode.erEndringPaaEksisterendePeriode) {
                utbetalingsperiode.forrigePeriodeId?.let {
                    refDelytelseId = utbetalingsoppdrag.saksnummer + it
                    refFagsystemId = utbetalingsoppdrag.saksnummer
                }
            }
            vedtakId = utbetalingsperiode.datoForVedtak.toString()
            delytelseId = utbetalingsoppdrag.saksnummer + utbetalingsperiode.periodeId
            kodeKlassifik = utbetalingsperiode.klassifisering
            datoVedtakFom = utbetalingsperiode.vedtakdatoFom.toXMLDate()
            datoVedtakTom = utbetalingsperiode.vedtakdatoTom.toXMLDate()
            sats = utbetalingsperiode.sats
            fradragTillegg = FRADRAG_TILLEGG
            typeSats = SatsTypeKode.fromKode(utbetalingsperiode.satsType.name).kode
            brukKjoreplan =
                if (utbetalingsoppdrag.gOmregning) BRUK_KJOEREPLAN_G_OMBEREGNING else BRUK_KJOEREPLAN_DEFAULT
            saksbehId = utbetalingsoppdrag.saksbehandlerId
            utbetalesTilId = utbetalingsperiode.utbetalesTil
            henvisning = utbetalingsperiode.behandlingId
            attestant180.add(
                Attestant180().withAttestantId(utbetalingsoppdrag.saksbehandlerId)
            )
        }
    }
}
