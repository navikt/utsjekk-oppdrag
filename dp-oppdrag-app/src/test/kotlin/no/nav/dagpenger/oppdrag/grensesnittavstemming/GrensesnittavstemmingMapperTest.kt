package no.nav.dagpenger.oppdrag.grensesnittavstemming

import no.nav.dagpenger.kontrakter.felles.Fagsystem
import no.nav.dagpenger.kontrakter.oppdrag.Utbetalingsoppdrag
import no.nav.dagpenger.oppdrag.util.TestOppdragMedAvstemmingsdato
import no.nav.dagpenger.oppdrag.util.somOppdragLager
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

internal class GrensesnittavstemmingMapperTest {
    @Test
    fun `test mapping av tom liste`() {
        val mapper =
            GrensesnittavstemmingMapper(emptyList(), Fagsystem.Dagpenger, LocalDateTime.now(), LocalDateTime.now())
        val meldinger = mapper.lagAvstemmingsmeldinger()

        assertEquals(0, meldinger.size)
    }

    @Test
    fun `test mapping til grensesnittavstemming`() {
        val avstemmingstidspunkt = LocalDateTime.now().minusDays(1).withHour(13)
        val fom = avstemmingstidspunkt.toLocalDate().atStartOfDay()
        val tom = avstemmingstidspunkt.toLocalDate().atTime(LocalTime.MAX)
        val utbetalingsoppdrag =
            TestOppdragMedAvstemmingsdato.lagTestUtbetalingsoppdrag(
                avstemmingstidspunkt,
                Fagsystem.Dagpenger,
            )
        val oppdragLager = utbetalingsoppdrag.somOppdragLager
        val mapper =
            GrensesnittavstemmingMapper(listOf(oppdragLager), Fagsystem.Dagpenger, fom, tom)
        val meldinger = mapper.lagAvstemmingsmeldinger()

        assertEquals(3, meldinger.size)
        assertAksjon(fom, tom, AksjonType.START, meldinger.first().aksjon)
        assertAksjon(fom, tom, AksjonType.DATA, meldinger[1].aksjon)
        assertAksjon(fom, tom, AksjonType.AVSL, meldinger.last().aksjon)

        assertDetaljData(utbetalingsoppdrag, meldinger[1].detalj.first())
        assertTotalData(utbetalingsoppdrag, meldinger[1].total)
        assertPeriodeData(utbetalingsoppdrag, meldinger[1].periode)
        assertGrunnlagsdata(utbetalingsoppdrag, meldinger[1].grunnlag)
    }

    @Test
    fun `tester at fom og tom blir satt riktig ved grensesnittavstemming`() {
        val førsteAvstemmingstidspunkt = LocalDateTime.now().minusDays(1).withHour(13)
        val andreAvstemmingstidspunkt = LocalDateTime.now().minusDays(1).withHour(15)
        val avstemmingFom = førsteAvstemmingstidspunkt.toLocalDate().atStartOfDay()
        val avstemmingTom = andreAvstemmingstidspunkt.toLocalDate().atTime(LocalTime.MAX)
        val baOppdragLager1 =
            TestOppdragMedAvstemmingsdato.lagTestUtbetalingsoppdrag(førsteAvstemmingstidspunkt, Fagsystem.Dagpenger).somOppdragLager
        val baOppdragLager2 =
            TestOppdragMedAvstemmingsdato.lagTestUtbetalingsoppdrag(andreAvstemmingstidspunkt, Fagsystem.Dagpenger).somOppdragLager
        val mapper =
            GrensesnittavstemmingMapper(
                listOf(baOppdragLager1, baOppdragLager2),
                Fagsystem.Dagpenger,
                avstemmingFom,
                avstemmingTom,
            )
        val meldinger = mapper.lagAvstemmingsmeldinger()
        assertEquals(3, meldinger.size)
        assertEquals(avstemmingFom.format(timeFormatter), meldinger.first().aksjon.nokkelFom)
        assertEquals(avstemmingTom.format(timeFormatter), meldinger.first().aksjon.nokkelTom)
    }

    private fun assertAksjon(
        avstemmingFom: LocalDateTime,
        avstemmingTom: LocalDateTime,
        expected: AksjonType,
        actual: Aksjonsdata,
    ) {
        assertEquals(expected, actual.aksjonType)
        assertEquals(KildeType.AVLEV, actual.kildeType)
        assertEquals(AvstemmingType.GRSN, actual.avstemmingType)
        assertEquals(Fagsystem.Dagpenger.kode, actual.avleverendeKomponentKode)
        assertEquals("OS", actual.mottakendeKomponentKode)
        assertEquals(Fagsystem.Dagpenger.kode, actual.underkomponentKode)
        assertEquals(avstemmingFom.format(timeFormatter), actual.nokkelFom)
        assertEquals(avstemmingTom.format(timeFormatter), actual.nokkelTom)
        assertEquals(Fagsystem.Dagpenger.kode, actual.brukerId)
    }

    private fun assertDetaljData(
        utbetalingsoppdrag: Utbetalingsoppdrag,
        actual: Detaljdata,
    ) {
        assertEquals(DetaljType.MANG, actual.detaljType)
        assertEquals(utbetalingsoppdrag.aktør, actual.offnr)
        assertEquals(Fagsystem.Dagpenger.kode, actual.avleverendeTransaksjonNokkel)
        assertEquals(utbetalingsoppdrag.avstemmingTidspunkt.format(timeFormatter), actual.tidspunkt)
        assertEquals(null, actual.meldingKode)
        assertEquals(null, actual.alvorlighetsgrad)
        assertEquals(null, actual.tekstMelding)
    }

    private fun assertTotalData(
        utbetalingsoppdrag: Utbetalingsoppdrag,
        actual: Totaldata,
    ) {
        assertEquals(1, actual.totalAntall)
        assertEquals(utbetalingsoppdrag.utbetalingsperiode.first().sats, actual.totalBelop)
        assertEquals(Fortegn.T, actual.fortegn)
    }

    private fun assertPeriodeData(
        utbetalingsoppdrag: Utbetalingsoppdrag,
        actual: Periodedata,
    ) {
        assertEquals(
            utbetalingsoppdrag.avstemmingTidspunkt.format(DateTimeFormatter.ofPattern("yyyyMMddHH")),
            actual.datoAvstemtFom,
        )
        assertEquals(
            utbetalingsoppdrag.avstemmingTidspunkt.format(DateTimeFormatter.ofPattern("yyyyMMddHH")),
            actual.datoAvstemtTom,
        )
    }

    private fun assertGrunnlagsdata(
        utbetalingsoppdrag: Utbetalingsoppdrag,
        actual: Grunnlagsdata,
    ) {
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

    companion object {
        private val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS")
    }
}
