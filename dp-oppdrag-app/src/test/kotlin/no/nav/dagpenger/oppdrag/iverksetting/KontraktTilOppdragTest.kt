package no.nav.dagpenger.oppdrag.iverksetting

import no.nav.dagpenger.kontrakter.felles.Fagsystem
import no.nav.dagpenger.kontrakter.felles.GeneriskIdSomUUID
import no.nav.dagpenger.kontrakter.felles.somUUID
import no.nav.dagpenger.kontrakter.oppdrag.Opphør
import no.nav.dagpenger.kontrakter.oppdrag.Utbetalingsoppdrag
import no.nav.dagpenger.kontrakter.oppdrag.Utbetalingsperiode
import no.nav.dagpenger.oppdrag.domene.tilKomprimertFagsystemId
import no.nav.dagpenger.oppdrag.iverksetting.domene.Endringskode
import no.nav.dagpenger.oppdrag.iverksetting.domene.OppdragMapper
import no.nav.dagpenger.oppdrag.iverksetting.domene.OppdragSkjemaConstants
import no.nav.dagpenger.oppdrag.iverksetting.domene.Utbetalingsfrekvens
import no.nav.dagpenger.oppdrag.iverksetting.domene.UuidKomprimator.komprimer
import no.nav.dagpenger.oppdrag.iverksetting.domene.fagsystemId
import no.nav.dagpenger.oppdrag.iverksetting.domene.toXMLDate
import no.trygdeetaten.skjema.oppdrag.Oppdrag110
import no.trygdeetaten.skjema.oppdrag.OppdragsLinje150
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class KontraktTilOppdragTest {
    private val iDag: LocalDate = LocalDate.now()

    @Test
    fun `mappe vedtaket til mari berg`() {
        val utbetalingsperiode1 =
            Utbetalingsperiode(
                erEndringPåEksisterendePeriode = false,
                opphør = null,
                periodeId = 1,
                forrigePeriodeId = null,
                datoForVedtak = iDag,
                klassifisering = "BATR",
                vedtakdatoFom = iDag,
                vedtakdatoTom = iDag.plusYears(6),
                sats = BigDecimal.valueOf(1354L),
                satsType = Utbetalingsperiode.SatsType.MND,
                utbetalesTil = "12345678911",
                behandlingId = GeneriskIdSomUUID(UUID.randomUUID()),
            )

        val utbetalingsperiode2 =
            Utbetalingsperiode(
                erEndringPåEksisterendePeriode = false,
                opphør = null,
                periodeId = 2,
                forrigePeriodeId = 1,
                datoForVedtak = iDag,
                klassifisering = "BATR",
                vedtakdatoFom = iDag.plusYears(6).plusMonths(1),
                vedtakdatoTom = iDag.plusYears(12).plusMonths(1),
                sats = BigDecimal.valueOf(1054L),
                satsType = Utbetalingsperiode.SatsType.MND,
                utbetalesTil = "12345678911",
                behandlingId = GeneriskIdSomUUID(UUID.randomUUID()),
                utbetalingsgrad = 60,
            )

        val utbetalingsoppdrag =
            Utbetalingsoppdrag(
                kodeEndring = Utbetalingsoppdrag.KodeEndring.NY,
                fagSystem = Fagsystem.Dagpenger,
                saksnummer = GeneriskIdSomUUID(UUID.randomUUID()),
                aktør = "12345678911",
                saksbehandlerId = "Z992991",
                utbetalingsperiode = listOf(utbetalingsperiode1, utbetalingsperiode2),
            )

        val oppdrag110 = OppdragMapper.tilOppdrag110(utbetalingsoppdrag)

        assertOppdrag110(utbetalingsoppdrag, oppdrag110)
        assertOppdragslinje150(utbetalingsperiode1, utbetalingsoppdrag, oppdrag110.oppdragsLinje150[0])
        assertOppdragslinje150(utbetalingsperiode2, utbetalingsoppdrag, oppdrag110.oppdragsLinje150[1])
    }

    @Test
    fun mappeOpphørPåVedtaketTilMariBerg() {
        val utbetalingsperiode1 =
            Utbetalingsperiode(
                erEndringPåEksisterendePeriode = true,
                opphør = Opphør(iDag.plusMonths(1)),
                periodeId = 3,
                forrigePeriodeId = 2,
                datoForVedtak = iDag,
                klassifisering = "BATR",
                vedtakdatoFom = iDag,
                vedtakdatoTom = iDag.plusYears(2),
                sats = BigDecimal.valueOf(1354L),
                satsType = Utbetalingsperiode.SatsType.MND,
                utbetalesTil = "12345678911",
                behandlingId = GeneriskIdSomUUID(UUID.randomUUID()),
            )
        val utbetalingsoppdrag =
            Utbetalingsoppdrag(
                kodeEndring = Utbetalingsoppdrag.KodeEndring.ENDR,
                fagSystem = Fagsystem.Dagpenger,
                saksnummer = GeneriskIdSomUUID(UUID.randomUUID()),
                aktør = "12345678911",
                saksbehandlerId = "Z992991",
                utbetalingsperiode = listOf(utbetalingsperiode1),
            )

        val oppdrag110 = OppdragMapper.tilOppdrag110(utbetalingsoppdrag)

        assertOppdrag110(utbetalingsoppdrag, oppdrag110)
        assertOppdragslinje150(utbetalingsperiode1, utbetalingsoppdrag, oppdrag110.oppdragsLinje150[0])
    }

    private fun assertOppdrag110(
        utbetalingsoppdrag: Utbetalingsoppdrag,
        oppdrag110: Oppdrag110,
    ) {
        Assertions.assertEquals(OppdragSkjemaConstants.KODE_AKSJON, oppdrag110.kodeAksjon)
        Assertions.assertEquals(utbetalingsoppdrag.kodeEndring.name, oppdrag110.kodeEndring.toString())
        Assertions.assertEquals(utbetalingsoppdrag.fagSystem.kode, oppdrag110.kodeFagomraade)
        Assertions.assertEquals(utbetalingsoppdrag.tilKomprimertFagsystemId(), oppdrag110.fagsystemId)
        Assertions.assertEquals(Utbetalingsfrekvens.MÅNEDLIG.kode, oppdrag110.utbetFrekvens)
        Assertions.assertEquals(utbetalingsoppdrag.aktør, oppdrag110.oppdragGjelderId)
        Assertions.assertEquals(OppdragSkjemaConstants.OPPDRAG_GJELDER_DATO_FOM.toXMLDate(), oppdrag110.datoOppdragGjelderFom)
        Assertions.assertEquals(utbetalingsoppdrag.saksbehandlerId, oppdrag110.saksbehId)
        Assertions.assertEquals(utbetalingsoppdrag.fagSystem.kode, oppdrag110.avstemming115.kodeKomponent)
        Assertions.assertEquals(
            utbetalingsoppdrag.avstemmingTidspunkt.format(OppdragMapper.timeFormatter),
            oppdrag110.avstemming115.nokkelAvstemming,
        )
        Assertions.assertEquals(
            utbetalingsoppdrag.avstemmingTidspunkt.format(OppdragMapper.timeFormatter),
            oppdrag110.avstemming115.tidspktMelding,
        )
        Assertions.assertEquals(OppdragSkjemaConstants.ENHET_TYPE_BOSTEDSENHET, oppdrag110.oppdragsEnhet120[0].typeEnhet)
        Assertions.assertEquals(OppdragSkjemaConstants.ENHET, oppdrag110.oppdragsEnhet120[0].enhet)
        Assertions.assertEquals(OppdragSkjemaConstants.ENHET_DATO_FOM.toXMLDate(), oppdrag110.oppdragsEnhet120[0].datoEnhetFom)
    }

    private fun assertOppdragslinje150(
        utbetalingsperiode: Utbetalingsperiode,
        utbetalingsoppdrag: Utbetalingsoppdrag,
        oppdragsLinje150: OppdragsLinje150,
    ) {
        Assertions.assertEquals(
            if (utbetalingsperiode.erEndringPåEksisterendePeriode) {
                Endringskode.ENDRING.kode
            } else {
                Endringskode.NY.kode
            },
            oppdragsLinje150.kodeEndringLinje,
        )
        assertOpphør(utbetalingsperiode, oppdragsLinje150)
        Assertions.assertEquals(utbetalingsperiode.datoForVedtak.toString(), oppdragsLinje150.vedtakId)
        Assertions.assertEquals(
            utbetalingsoppdrag.tilKomprimertFagsystemId() + "#" + utbetalingsperiode.periodeId.toString(),
            oppdragsLinje150.delytelseId,
        )
        Assertions.assertEquals(utbetalingsperiode.klassifisering, oppdragsLinje150.kodeKlassifik)
        Assertions.assertEquals(utbetalingsperiode.vedtakdatoFom.toXMLDate(), oppdragsLinje150.datoVedtakFom)
        Assertions.assertEquals(utbetalingsperiode.vedtakdatoTom.toXMLDate(), oppdragsLinje150.datoVedtakTom)
        Assertions.assertEquals(utbetalingsperiode.sats, oppdragsLinje150.sats)
        Assertions.assertEquals(OppdragSkjemaConstants.FRADRAG_TILLEGG, oppdragsLinje150.fradragTillegg)
        Assertions.assertEquals(utbetalingsperiode.satsType.name, oppdragsLinje150.typeSats)
        Assertions.assertEquals(OppdragSkjemaConstants.BRUK_KJØREPLAN_DEFAULT, oppdragsLinje150.brukKjoreplan)
        Assertions.assertEquals(utbetalingsoppdrag.saksbehandlerId, oppdragsLinje150.saksbehId)
        Assertions.assertEquals(utbetalingsoppdrag.aktør, oppdragsLinje150.utbetalesTilId)
        Assertions.assertEquals(utbetalingsperiode.behandlingId.somUUID.komprimer(), oppdragsLinje150.henvisning)
        Assertions.assertEquals(utbetalingsoppdrag.saksbehandlerId, oppdragsLinje150.attestant180[0].attestantId)
        Assertions.assertEquals(utbetalingsperiode.utbetalingsgrad, oppdragsLinje150.grad170.firstOrNull()?.grad?.toInt())

        if (utbetalingsperiode.forrigePeriodeId !== null && !utbetalingsperiode.erEndringPåEksisterendePeriode) {
            Assertions.assertEquals(
                utbetalingsoppdrag.tilKomprimertFagsystemId() + "#" + utbetalingsperiode.forrigePeriodeId.toString(),
                oppdragsLinje150.refDelytelseId,
            )
        }
    }

    private fun assertOpphør(
        utbetalingsperiode: Utbetalingsperiode,
        oppdragsLinje150: OppdragsLinje150,
    ) {
        if (utbetalingsperiode.opphør == null) {
            Assertions.assertEquals(utbetalingsperiode.opphør, oppdragsLinje150.kodeStatusLinje)
            Assertions.assertEquals(utbetalingsperiode.opphør, oppdragsLinje150.datoStatusFom)
        } else {
            utbetalingsperiode.opphør?.let {
                Assertions.assertEquals("OPPH", oppdragsLinje150.kodeStatusLinje.name)
                Assertions.assertEquals(it.opphørDatoFom.toXMLDate(), oppdragsLinje150.datoStatusFom)
            }
        }
    }
}
