package no.nav.dagpenger.oppdrag.iverksetting

import no.nav.dagpenger.kontrakter.felles.Fagsystem
import no.nav.dagpenger.kontrakter.felles.GeneriskIdSomUUID
import no.nav.dagpenger.kontrakter.felles.Satstype
import no.nav.dagpenger.kontrakter.felles.somUUID
import no.nav.dagpenger.kontrakter.oppdrag.Opphør
import no.nav.dagpenger.kontrakter.oppdrag.Utbetalingsoppdrag
import no.nav.dagpenger.kontrakter.oppdrag.Utbetalingsperiode
import no.nav.dagpenger.oppdrag.iverksetting.domene.Endringskode
import no.nav.dagpenger.oppdrag.iverksetting.domene.OppdragMapper
import no.nav.dagpenger.oppdrag.iverksetting.domene.OppdragSkjemaConstants
import no.nav.dagpenger.oppdrag.iverksetting.domene.Utbetalingsfrekvens
import no.nav.dagpenger.oppdrag.iverksetting.domene.UuidKomprimator.komprimer
import no.nav.dagpenger.oppdrag.iverksetting.domene.komprimertFagsystemId
import no.nav.dagpenger.oppdrag.iverksetting.domene.toXMLDate
import no.trygdeetaten.skjema.oppdrag.Oppdrag110
import no.trygdeetaten.skjema.oppdrag.OppdragsLinje150
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class OppdragMapperTest {
    @Test
    fun `mappe vedtak`() {
        val utbetalingsperiode1 =
            Utbetalingsperiode(
                erEndringPåEksisterendePeriode = false,
                opphør = null,
                periodeId = 1,
                forrigePeriodeId = null,
                vedtaksdato = LocalDate.now(),
                klassifisering = "BATR",
                fom = LocalDate.now(),
                tom = LocalDate.now().plusYears(6),
                sats = BigDecimal.valueOf(1354L),
                satstype = Satstype.MÅNEDLIG,
                utbetalesTil = "12345678911",
                behandlingId = GeneriskIdSomUUID(UUID.randomUUID()),
            )

        val utbetalingsperiode2 =
            Utbetalingsperiode(
                erEndringPåEksisterendePeriode = false,
                opphør = null,
                periodeId = 2,
                forrigePeriodeId = 1,
                vedtaksdato = LocalDate.now(),
                klassifisering = "BATR",
                fom = LocalDate.now().plusYears(6).plusMonths(1),
                tom = LocalDate.now().plusYears(12).plusMonths(1),
                sats = BigDecimal.valueOf(1054L),
                satstype = Satstype.MÅNEDLIG,
                utbetalesTil = "12345678911",
                behandlingId = GeneriskIdSomUUID(UUID.randomUUID()),
                utbetalingsgrad = 60,
            )

        val utbetalingsoppdrag =
            Utbetalingsoppdrag(
                erFørsteUtbetalingPåSak = true,
                fagsystem = Fagsystem.DAGPENGER,
                saksnummer = GeneriskIdSomUUID(UUID.randomUUID()),
                aktør = "12345678911",
                saksbehandlerId = "Z992991",
                utbetalingsperiode = listOf(utbetalingsperiode1, utbetalingsperiode2),
                iverksettingId = null,
            )

        val oppdrag110 = OppdragMapper.tilOppdrag110(utbetalingsoppdrag)

        assertOppdrag110(utbetalingsoppdrag, oppdrag110)
        assertOppdragslinje150(utbetalingsperiode1, utbetalingsoppdrag, oppdrag110.oppdragsLinje150[0])
        assertOppdragslinje150(utbetalingsperiode2, utbetalingsoppdrag, oppdrag110.oppdragsLinje150[1])
    }

    @Test
    fun `mappe opphør på vedtak`() {
        val utbetalingsperiode1 =
            Utbetalingsperiode(
                erEndringPåEksisterendePeriode = true,
                opphør = Opphør(LocalDate.now().plusMonths(1)),
                periodeId = 3,
                forrigePeriodeId = 2,
                vedtaksdato = LocalDate.now(),
                klassifisering = "BATR",
                fom = LocalDate.now(),
                tom = LocalDate.now().plusYears(2),
                sats = BigDecimal.valueOf(1354L),
                satstype = Satstype.MÅNEDLIG,
                utbetalesTil = "12345678911",
                behandlingId = GeneriskIdSomUUID(UUID.randomUUID()),
            )
        val utbetalingsoppdrag =
            Utbetalingsoppdrag(
                erFørsteUtbetalingPåSak = false,
                fagsystem = Fagsystem.DAGPENGER,
                saksnummer = GeneriskIdSomUUID(UUID.randomUUID()),
                aktør = "12345678911",
                saksbehandlerId = "Z992991",
                utbetalingsperiode = listOf(utbetalingsperiode1),
                iverksettingId = null,
            )

        val oppdrag110 = OppdragMapper.tilOppdrag110(utbetalingsoppdrag)

        assertOppdrag110(utbetalingsoppdrag, oppdrag110)
        assertOppdragslinje150(utbetalingsperiode1, utbetalingsoppdrag, oppdrag110.oppdragsLinje150[0])
    }

    @Test
    fun `mappe vedtak for tiltakspenger`() {
        val utbetalingsperiode1 =
            Utbetalingsperiode(
                erEndringPåEksisterendePeriode = false,
                opphør = null,
                periodeId = 1,
                forrigePeriodeId = null,
                vedtaksdato = LocalDate.now(),
                klassifisering = "TPTPATT",
                fom = LocalDate.now(),
                tom = LocalDate.now().plusDays(6),
                sats = BigDecimal.valueOf(1354L),
                satstype = Satstype.DAGLIG,
                utbetalesTil = "12345678911",
                behandlingId = GeneriskIdSomUUID(UUID.randomUUID()),
            )
        val utbetalingsoppdrag =
            Utbetalingsoppdrag(
                erFørsteUtbetalingPåSak = true,
                fagsystem = Fagsystem.TILTAKSPENGER,
                saksnummer = GeneriskIdSomUUID(UUID.randomUUID()),
                aktør = "12345678911",
                saksbehandlerId = "Z992991",
                brukersNavKontor = "0220",
                utbetalingsperiode = listOf(utbetalingsperiode1),
                iverksettingId = null,
            )

        val oppdrag110 = OppdragMapper.tilOppdrag110(utbetalingsoppdrag)

        assertOppdrag110(utbetalingsoppdrag, oppdrag110)
        assertOppdragslinje150(utbetalingsperiode1, utbetalingsoppdrag, oppdrag110.oppdragsLinje150[0])
    }

    private fun assertOppdrag110(
        utbetalingsoppdrag: Utbetalingsoppdrag,
        oppdrag110: Oppdrag110,
    ) {
        val forventetEndringskode = if (utbetalingsoppdrag.erFørsteUtbetalingPåSak) Endringskode.NY else Endringskode.ENDRING
        assertEquals(OppdragSkjemaConstants.KODE_AKSJON, oppdrag110.kodeAksjon)
        assertEquals(forventetEndringskode.kode, oppdrag110.kodeEndring.toString())
        assertEquals(utbetalingsoppdrag.fagsystem.kode, oppdrag110.kodeFagomraade)
        assertEquals(utbetalingsoppdrag.komprimertFagsystemId, oppdrag110.fagsystemId)
        assertEquals(Utbetalingsfrekvens.MÅNEDLIG.kode, oppdrag110.utbetFrekvens)
        assertEquals(utbetalingsoppdrag.aktør, oppdrag110.oppdragGjelderId)
        assertEquals(OppdragSkjemaConstants.OPPDRAG_GJELDER_DATO_FOM.toXMLDate(), oppdrag110.datoOppdragGjelderFom)
        assertEquals(utbetalingsoppdrag.saksbehandlerId, oppdrag110.saksbehId)
        assertEquals(utbetalingsoppdrag.fagsystem.kode, oppdrag110.avstemming115.kodeKomponent)
        assertEquals(
            utbetalingsoppdrag.avstemmingstidspunkt.format(OppdragMapper.timeFormatter),
            oppdrag110.avstemming115.nokkelAvstemming,
        )
        assertEquals(
            utbetalingsoppdrag.avstemmingstidspunkt.format(OppdragMapper.timeFormatter),
            oppdrag110.avstemming115.tidspktMelding,
        )
        assertEquals(OppdragSkjemaConstants.ENHET_TYPE_BOSTEDSENHET, oppdrag110.oppdragsEnhet120[0].typeEnhet)
        utbetalingsoppdrag.brukersNavKontor?.let {
            assertEquals(it, oppdrag110.oppdragsEnhet120[0].enhet)
            assertEquals(OppdragSkjemaConstants.BRUKERS_NAVKONTOR_FOM.toXMLDate(), oppdrag110.oppdragsEnhet120[0].datoEnhetFom)
            assertEquals(OppdragSkjemaConstants.ENHET_TYPE_BEHANDLENDE_ENHET, oppdrag110.oppdragsEnhet120[1].typeEnhet)
            assertEquals(OppdragSkjemaConstants.ENHET, oppdrag110.oppdragsEnhet120[1].enhet)
            assertEquals(OppdragSkjemaConstants.ENHET_FOM.toXMLDate(), oppdrag110.oppdragsEnhet120[1].datoEnhetFom)
        }
            ?: {
                assertEquals(OppdragSkjemaConstants.ENHET, oppdrag110.oppdragsEnhet120[0].enhet)
                assertEquals(OppdragSkjemaConstants.ENHET_FOM.toXMLDate(), oppdrag110.oppdragsEnhet120[0].datoEnhetFom)
            }
    }

    private fun assertOppdragslinje150(
        utbetalingsperiode: Utbetalingsperiode,
        utbetalingsoppdrag: Utbetalingsoppdrag,
        oppdragsLinje150: OppdragsLinje150,
    ) {
        assertEquals(
            if (utbetalingsperiode.erEndringPåEksisterendePeriode) {
                Endringskode.ENDRING.kode
            } else {
                Endringskode.NY.kode
            },
            oppdragsLinje150.kodeEndringLinje,
        )
        assertOpphør(utbetalingsperiode, oppdragsLinje150)
        assertEquals(utbetalingsperiode.vedtaksdato.toString(), oppdragsLinje150.vedtakId)
        assertEquals(
            utbetalingsoppdrag.komprimertFagsystemId + "#" + utbetalingsperiode.periodeId.toString(),
            oppdragsLinje150.delytelseId,
        )
        assertEquals(utbetalingsperiode.klassifisering, oppdragsLinje150.kodeKlassifik)
        assertEquals(utbetalingsperiode.fom.toXMLDate(), oppdragsLinje150.datoVedtakFom)
        assertEquals(utbetalingsperiode.tom.toXMLDate(), oppdragsLinje150.datoVedtakTom)
        assertEquals(utbetalingsperiode.sats, oppdragsLinje150.sats)
        assertEquals(OppdragSkjemaConstants.FRADRAG_TILLEGG, oppdragsLinje150.fradragTillegg)
        assertEquals(OppdragSkjemaConstants.BRUK_KJØREPLAN_DEFAULT, oppdragsLinje150.brukKjoreplan)
        assertEquals(utbetalingsoppdrag.saksbehandlerId, oppdragsLinje150.saksbehId)
        assertEquals(utbetalingsoppdrag.aktør, oppdragsLinje150.utbetalesTilId)
        assertEquals(utbetalingsperiode.behandlingId.somUUID.komprimer(), oppdragsLinje150.henvisning)
        assertEquals(utbetalingsoppdrag.saksbehandlerId, oppdragsLinje150.attestant180[0].attestantId)
        assertEquals(utbetalingsperiode.utbetalingsgrad, oppdragsLinje150.grad170.firstOrNull()?.grad?.toInt())

        if (utbetalingsperiode.forrigePeriodeId !== null && !utbetalingsperiode.erEndringPåEksisterendePeriode) {
            assertEquals(
                utbetalingsoppdrag.komprimertFagsystemId + "#" + utbetalingsperiode.forrigePeriodeId.toString(),
                oppdragsLinje150.refDelytelseId,
            )
        }
    }

    private fun assertOpphør(
        utbetalingsperiode: Utbetalingsperiode,
        oppdragsLinje150: OppdragsLinje150,
    ) {
        utbetalingsperiode.opphør.let { opphør ->
            if (opphør == null) {
                assertNull(oppdragsLinje150.kodeStatusLinje)
                assertNull(oppdragsLinje150.datoStatusFom)
            } else {
                assertEquals("OPPH", oppdragsLinje150.kodeStatusLinje.name)
                assertEquals(opphør.fom.toXMLDate(), oppdragsLinje150.datoStatusFom)
            }
        }
    }
}
