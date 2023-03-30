package dp.oppdrag

import dp.oppdrag.model.*
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
            enhet = OppdragSkjemaConstants.ENHET
            typeEnhet = OppdragSkjemaConstants.ENHET_TYPE
            datoEnhetFom = OppdragSkjemaConstants.ENHET_DATO_FOM.toXMLDate()
        }

        val oppdrag110 = objectFactory.createOppdrag110().apply {
            kodeAksjon = OppdragSkjemaConstants.KODE_AKSJON
            kodeEndring = EndringsKode.fromKode(utbetalingsoppdrag.kodeEndring.name).kode
            kodeFagomraade = utbetalingsoppdrag.fagSystem
            fagsystemId = utbetalingsoppdrag.saksnummer
            utbetFrekvens = UtbetalingsfrekvensKode.MAANEDLIG.kode
            oppdragGjelderId = utbetalingsoppdrag.aktoer
            datoOppdragGjelderFom = OppdragSkjemaConstants.OPPDRAG_GJELDER_DATO_FOM.toXMLDate()
            saksbehId = utbetalingsoppdrag.saksbehandlerId
            bilagstype113 = Bilagstype113().withBilagsType("U")
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

        val attestant = objectFactory.createAttestant180().apply {
            attestantId = utbetalingsoppdrag.saksbehandlerId
        }

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
            fradragTillegg = OppdragSkjemaConstants.FRADRAG_TILLEGG
            typeSats = SatsTypeKode.fromKode(utbetalingsperiode.satsType.name).kode
            brukKjoreplan = if (utbetalingsoppdrag.gOmregning)
                OppdragSkjemaConstants.BRUK_KJOEREPLAN_G_OMBEREGNING else OppdragSkjemaConstants.BRUK_KJOEREPLAN_DEFAULT
            saksbehId = utbetalingsoppdrag.saksbehandlerId
            utbetalesTilId = utbetalingsperiode.utbetalesTil
            henvisning = utbetalingsperiode.behandlingId.toString()
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
}
