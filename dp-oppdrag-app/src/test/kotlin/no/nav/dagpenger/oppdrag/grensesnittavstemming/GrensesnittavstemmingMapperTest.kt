package no.nav.dagpenger.oppdrag.grensesnittavstemming

import no.nav.dagpenger.kontrakter.felles.Fagsystem
import no.nav.dagpenger.kontrakter.oppdrag.Utbetalingsoppdrag
import no.nav.dagpenger.oppdrag.avstemming.SystemKode
import no.nav.dagpenger.oppdrag.repository.somOppdragLager
import no.nav.dagpenger.oppdrag.util.TestOppdragMedAvstemmingsdato
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.AksjonType
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Aksjonsdata
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.AvstemmingType
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.DetaljType
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Detaljdata
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Fortegn
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Grunnlagsdata
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.KildeType
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Periodedata
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Totaldata
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.test.assertEquals

class GrensesnittavstemmingMapperTest {
    val fagsystem = Fagsystem.Dagpenger
    val tidspunktFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS")

    @Test
    fun testMappingAvTomListe() {
        val mapper = GrensesnittavstemmingMapper(emptyList(), fagsystem, LocalDateTime.now(), LocalDateTime.now())
        val meldinger = mapper.lagAvstemmingsmeldinger()
        assertEquals(0, meldinger.size)
    }

    @Test
    fun testMappingTilGrensesnittavstemming() {
        val avstemmingstidspunkt = LocalDateTime.now().minusDays(1).withHour(13)
        val avstemmingFom = avstemmingstidspunkt.toLocalDate().atStartOfDay()
        val avstemmingTom = avstemmingstidspunkt.toLocalDate().atTime(LocalTime.MAX)
        val utbetalingsoppdrag = TestOppdragMedAvstemmingsdato.lagTestUtbetalingsoppdrag(avstemmingstidspunkt, fagsystem)
        val oppdragLager = utbetalingsoppdrag.somOppdragLager
        val mapper = GrensesnittavstemmingMapper(listOf(oppdragLager), fagsystem, avstemmingFom, avstemmingTom)
        val meldinger = mapper.lagAvstemmingsmeldinger()
        assertEquals(3, meldinger.size)
        assertAksjon(avstemmingFom, avstemmingTom, AksjonType.START, meldinger.first().aksjon)
        assertAksjon(avstemmingFom, avstemmingTom, AksjonType.DATA, meldinger[1].aksjon)
        assertAksjon(avstemmingFom, avstemmingTom, AksjonType.AVSL, meldinger.last().aksjon)

        assertDetaljData(utbetalingsoppdrag, meldinger[1].detalj.first())
        assertTotalData(utbetalingsoppdrag, meldinger[1].total)
        assertPeriodeData(utbetalingsoppdrag, meldinger[1].periode)
        assertGrunnlagsdata(utbetalingsoppdrag, meldinger[1].grunnlag)
    }

    @Test
    fun testerAtFomOgTomBlirSattRiktigVedGrensesnittavstemming() {
        val førsteAvstemmingstidspunkt = LocalDateTime.now().minusDays(1).withHour(13)
        val andreAvstemmingstidspunkt = LocalDateTime.now().minusDays(1).withHour(15)
        val avstemmingFom = førsteAvstemmingstidspunkt.toLocalDate().atStartOfDay()
        val avstemmingTom = andreAvstemmingstidspunkt.toLocalDate().atTime(LocalTime.MAX)
        val baOppdragLager1 =
            TestOppdragMedAvstemmingsdato.lagTestUtbetalingsoppdrag(førsteAvstemmingstidspunkt, fagsystem).somOppdragLager
        val baOppdragLager2 =
            TestOppdragMedAvstemmingsdato.lagTestUtbetalingsoppdrag(andreAvstemmingstidspunkt, fagsystem).somOppdragLager
        val mapper =
            GrensesnittavstemmingMapper(listOf(baOppdragLager1, baOppdragLager2), fagsystem, avstemmingFom, avstemmingTom)
        val meldinger = mapper.lagAvstemmingsmeldinger()
        assertEquals(3, meldinger.size)
        assertEquals(avstemmingFom.format(tidspunktFormatter), meldinger.first().aksjon.nokkelFom)
        assertEquals(avstemmingTom.format(tidspunktFormatter), meldinger.first().aksjon.nokkelTom)
    }

    fun assertAksjon(
        avstemmingFom: LocalDateTime,
        avstemmingTom: LocalDateTime,
        expected: AksjonType,
        actual: Aksjonsdata
    ) {
        assertEquals(expected, actual.aksjonType)
        assertEquals(KildeType.AVLEV, actual.kildeType)
        assertEquals(AvstemmingType.GRSN, actual.avstemmingType)
        assertEquals(fagsystem.kode, actual.avleverendeKomponentKode)
        assertEquals(SystemKode.OPPDRAGSSYSTEMET.kode, actual.mottakendeKomponentKode)
        assertEquals(fagsystem.kode, actual.underkomponentKode)
        assertEquals(avstemmingFom.format(tidspunktFormatter), actual.nokkelFom)
        assertEquals(avstemmingTom.format(tidspunktFormatter), actual.nokkelTom)
        assertEquals(fagsystem.kode, actual.brukerId)
    }

    fun assertDetaljData(utbetalingsoppdrag: Utbetalingsoppdrag, actual: Detaljdata) {
        assertEquals(DetaljType.MANG, actual.detaljType)
        assertEquals(utbetalingsoppdrag.aktoer, actual.offnr)
        assertEquals(fagsystem.kode, actual.avleverendeTransaksjonNokkel)
        assertEquals(utbetalingsoppdrag.avstemmingTidspunkt.format(tidspunktFormatter), actual.tidspunkt)
        assertEquals(null, actual.meldingKode)
        assertEquals(null, actual.alvorlighetsgrad)
        assertEquals(null, actual.tekstMelding)
    }

    fun assertTotalData(utbetalingsoppdrag: Utbetalingsoppdrag, actual: Totaldata) {
        assertEquals(1, actual.totalAntall)
        assertEquals(utbetalingsoppdrag.utbetalingsperiode.first().sats, actual.totalBelop)
        assertEquals(Fortegn.T, actual.fortegn)
    }

    fun assertPeriodeData(utbetalingsoppdrag: Utbetalingsoppdrag, actual: Periodedata) {
        assertEquals(
            utbetalingsoppdrag.avstemmingTidspunkt.format(DateTimeFormatter.ofPattern("yyyyMMddHH")),
            actual.datoAvstemtFom
        )
        assertEquals(
            utbetalingsoppdrag.avstemmingTidspunkt.format(DateTimeFormatter.ofPattern("yyyyMMddHH")),
            actual.datoAvstemtTom
        )
    }

    fun assertGrunnlagsdata(utbetalingsoppdrag: Utbetalingsoppdrag, actual: Grunnlagsdata) {
        assertEquals(1, actual.manglerAntall)
        assertEquals(utbetalingsoppdrag.utbetalingsperiode.first().sats, actual.manglerBelop)
        assertEquals(Fortegn.T, actual.manglerFortegn)

        assertEquals(0, actual.godkjentAntall)
        assertEquals(BigDecimal.ZERO, actual.godkjentBelop)
        assertEquals(Fortegn.T, actual.godkjentFortegn)

        assertEquals(0, actual.avvistAntall)
        assertEquals(BigDecimal.ZERO, actual.avvistBelop)
        assertEquals(Fortegn.T, actual.avvistFortegn)
    }
}