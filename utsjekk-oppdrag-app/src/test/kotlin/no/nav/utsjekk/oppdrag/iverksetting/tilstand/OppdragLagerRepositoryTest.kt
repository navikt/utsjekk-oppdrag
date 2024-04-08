package no.nav.utsjekk.oppdrag.iverksetting.tilstand

import no.nav.utsjekk.kontrakter.felles.Fagsystem
import no.nav.utsjekk.kontrakter.felles.tilFagsystem
import no.nav.utsjekk.kontrakter.oppdrag.OppdragStatus
import no.nav.utsjekk.oppdrag.PostgreSQLInitializer
import no.nav.utsjekk.oppdrag.config.DatabaseConfiguration
import no.nav.utsjekk.oppdrag.etUtbetalingsoppdrag
import no.nav.utsjekk.oppdrag.iverksetting.mq.OppdragXmlMapper
import no.nav.utsjekk.oppdrag.somOppdragLager
import no.trygdeetaten.skjema.oppdrag.Mmel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest
import org.springframework.dao.DuplicateKeyException
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(
    initializers = [PostgreSQLInitializer::class],
    classes = [DatabaseConfiguration::class, OppdragLagerTestConfig::class],
)
@Testcontainers
internal class OppdragLagerRepositoryTest {
    @Autowired
    lateinit var oppdragLagerRepository: OppdragLagerRepository

    companion object {
        @Container
        val postgreSQLContainer = PostgreSQLInitializer.container
    }

    @Test
    fun `skal ikke lagre duplikat`() {
        val oppdragLager = etUtbetalingsoppdrag().somOppdragLager

        oppdragLagerRepository.opprettOppdrag(oppdragLager)

        assertThrows<DuplicateKeyException> {
            oppdragLagerRepository.opprettOppdrag(oppdragLager)
        }
    }

    @Test
    fun `skal lagre to ulike iverksettinger samme behandling`() {
        val oppdragLager = etUtbetalingsoppdrag().somOppdragLager.copy(iverksettingId = "1")

        oppdragLagerRepository.opprettOppdrag(oppdragLager)

        assertDoesNotThrow {
            oppdragLagerRepository.opprettOppdrag(oppdragLager.copy(iverksettingId = "2"))
        }
    }

    @Test
    fun `skal lagre status`() {
        val oppdragLager =
            etUtbetalingsoppdrag().somOppdragLager
                .copy(status = OppdragStatus.LAGT_PÅ_KØ)

        oppdragLagerRepository.opprettOppdrag(oppdragLager)

        val hentetOppdrag = oppdragLagerRepository.hentOppdrag(oppdragLager.id)
        assertEquals(OppdragStatus.LAGT_PÅ_KØ, hentetOppdrag.status)

        oppdragLagerRepository.oppdaterStatus(hentetOppdrag.id, OppdragStatus.KVITTERT_OK)

        val hentetOppdatertOppdrag = oppdragLagerRepository.hentOppdrag(hentetOppdrag.id)
        assertEquals(OppdragStatus.KVITTERT_OK, hentetOppdatertOppdrag.status)
    }

    @Test
    fun `skal lagre kvitteringsmelding`() {
        val oppdragLager =
            etUtbetalingsoppdrag().somOppdragLager
                .copy(status = OppdragStatus.LAGT_PÅ_KØ)

        oppdragLagerRepository.opprettOppdrag(oppdragLager)
        val hentetOppdrag = oppdragLagerRepository.hentOppdrag(oppdragLager.id)
        val kvitteringsmelding = avvistKvitteringsmelding()

        oppdragLagerRepository.oppdaterKvitteringsmelding(hentetOppdrag.id, kvitteringsmelding)

        val hentetOppdatertOppdrag = oppdragLagerRepository.hentOppdrag(oppdragLager.id)

        assertTrue(kvitteringsmelding.erLik(hentetOppdatertOppdrag.kvitteringsmelding!!))
    }

    @Test
    fun `skal kun hente ut ett dp oppdrag for grensesnittavstemming`() {
        val dag = LocalDateTime.now()
        val startenPåDagen = dag.withHour(0).withMinute(0)
        val sluttenAvDagen = dag.withHour(23).withMinute(59)

        val baOppdragLager = etUtbetalingsoppdrag(dag).somOppdragLager
        val baOppdragLager2 = etUtbetalingsoppdrag(dag.minusDays(1)).somOppdragLager

        oppdragLagerRepository.opprettOppdrag(baOppdragLager)
        oppdragLagerRepository.opprettOppdrag(baOppdragLager2)

        val oppdrageneTilGrensesnittavstemming =
            oppdragLagerRepository.hentIverksettingerForGrensesnittavstemming(
                startenPåDagen,
                sluttenAvDagen,
                Fagsystem.DAGPENGER,
            )

        assertEquals(1, oppdrageneTilGrensesnittavstemming.size)
        assertEquals("DP", oppdrageneTilGrensesnittavstemming.first().fagsystem)
        assertEquals(
            dag.format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss")),
            oppdrageneTilGrensesnittavstemming.first().avstemmingTidspunkt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss")),
        )
    }

    private fun avvistKvitteringsmelding() =
        OppdragXmlMapper.tilOppdrag(
            this::class.java.getResourceAsStream("/kvittering-avvist.xml")
                ?.bufferedReader().use { it?.readText() ?: "" },
        ).mmel

    private val OppdragLager.id: OppdragId
        get() {
            return OppdragId(
                fagsystem = this.fagsystem.tilFagsystem(),
                fagsakId = this.fagsakId,
                behandlingId = this.behandlingId,
                iverksettingId = this.iverksettingId,
            )
        }

    private fun Mmel.erLik(andre: Mmel) =
        systemId == andre.systemId &&
            kodeMelding == andre.kodeMelding &&
            alvorlighetsgrad == andre.alvorlighetsgrad &&
            beskrMelding == andre.beskrMelding &&
            sqlKode == andre.sqlKode &&
            sqlState == andre.sqlState &&
            sqlMelding == andre.sqlMelding &&
            mqCompletionKode == andre.mqCompletionKode &&
            mqReasonKode == andre.mqReasonKode &&
            programId == andre.programId &&
            sectionNavn == andre.sectionNavn
}
