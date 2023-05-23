package no.nav.dagpenger.oppdrag.repository

import no.nav.dagpenger.kontrakter.utbetaling.Fagsystem
import no.nav.dagpenger.oppdrag.config.DatabaseConfiguration
import no.nav.dagpenger.oppdrag.domene.OppdragStatus
import no.nav.dagpenger.oppdrag.iverksetting.Jaxb
import no.nav.dagpenger.oppdrag.util.Containers
import no.nav.dagpenger.oppdrag.util.TestOppdragMedAvstemmingsdato
import no.nav.dagpenger.oppdrag.util.TestUtbetalingsoppdrag.utbetalingsoppdragMedTilfeldigAktoer
import no.trygdeetaten.skjema.oppdrag.Mmel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest
import org.springframework.dao.DuplicateKeyException
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.test.assertFailsWith

@JdbcTest
@ActiveProfiles("jdbc-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = [Containers.PostgresSQLInitializer::class], classes = [DatabaseConfiguration::class, OppdragLagerTestConfig::class])
@Testcontainers
internal class OppdragLagerRepositoryJdbcTest {

    @Autowired lateinit var oppdragLagerRepository: OppdragLagerRepository

    companion object {
        @Container var postgreSQLContainer = Containers.postgreSQLContainer
    }

    @Test
    fun skal_ikke_lagre_duplikat() {

        val oppdragLager = utbetalingsoppdragMedTilfeldigAktoer().somOppdragLager

        oppdragLagerRepository.opprettOppdrag(oppdragLager)

        assertFailsWith<DuplicateKeyException> {
            oppdragLagerRepository.opprettOppdrag(oppdragLager)
        }
    }

    @Test
    fun skal_lagre_status() {

        val oppdragLager = utbetalingsoppdragMedTilfeldigAktoer().somOppdragLager
            .copy(status = OppdragStatus.LAGT_PAA_KOE)

        oppdragLagerRepository.opprettOppdrag(oppdragLager)

        val hentetOppdrag = oppdragLagerRepository.hentOppdrag(oppdragLager.id)
        assertEquals(OppdragStatus.LAGT_PAA_KOE, hentetOppdrag.status)

        oppdragLagerRepository.oppdaterStatus(hentetOppdrag.id, OppdragStatus.KVITTERT_OK)

        val hentetOppdatertOppdrag = oppdragLagerRepository.hentOppdrag(hentetOppdrag.id)
        assertEquals(OppdragStatus.KVITTERT_OK, hentetOppdatertOppdrag.status)
    }

    @Test
    fun skal_lagre_kvitteringsmelding() {
        val oppdragLager = utbetalingsoppdragMedTilfeldigAktoer().somOppdragLager
            .copy(status = OppdragStatus.LAGT_PAA_KOE)

        oppdragLagerRepository.opprettOppdrag(oppdragLager)
        val hentetOppdrag = oppdragLagerRepository.hentOppdrag(oppdragLager.id)
        val kvitteringsmelding = kvitteringsmelding()

        oppdragLagerRepository.oppdaterKvitteringsmelding(hentetOppdrag.id, kvitteringsmelding)

        val hentetOppdatertOppdrag = oppdragLagerRepository.hentOppdrag(oppdragLager.id)
        assertThat(kvitteringsmelding).isEqualToComparingFieldByField(hentetOppdatertOppdrag.kvitteringsmelding)
    }

    private fun kvitteringsmelding(): Mmel {
        val kvitteringsmelding = Jaxb.tilOppdrag(
            this::class.java.getResourceAsStream("/kvittering-avvist.xml")
                .bufferedReader().use { it.readText() }
        )
        return kvitteringsmelding.mmel
    }

    @Test
    fun skal_kun_hente_ut_ett_DP_oppdrag_for_grensesnittavstemming() {
        val dag = LocalDateTime.now()
        val startenPåDagen = dag.withHour(0).withMinute(0)
        val sluttenAvDagen = dag.withHour(23).withMinute(59)

        val avstemmingsTidspunktetSomSkalKjøres = dag

        val baOppdragLager = TestOppdragMedAvstemmingsdato.lagTestUtbetalingsoppdrag(avstemmingsTidspunktetSomSkalKjøres).somOppdragLager
        val baOppdragLager2 = TestOppdragMedAvstemmingsdato.lagTestUtbetalingsoppdrag(dag.minusDays(1)).somOppdragLager

        oppdragLagerRepository.opprettOppdrag(baOppdragLager)
        oppdragLagerRepository.opprettOppdrag(baOppdragLager2)

        val oppdrageneTilGrensesnittavstemming = oppdragLagerRepository.hentIverksettingerForGrensesnittavstemming(startenPåDagen, sluttenAvDagen, Fagsystem.Dagpenger)

        assertEquals(1, oppdrageneTilGrensesnittavstemming.size)
        assertEquals("DP", oppdrageneTilGrensesnittavstemming.first().fagsystem)
        assertEquals(
            avstemmingsTidspunktetSomSkalKjøres.format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss")),
            oppdrageneTilGrensesnittavstemming.first().avstemmingTidspunkt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss"))
        )
    }
}
