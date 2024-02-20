package no.nav.dagpenger.oppdrag.iverksetting

import no.nav.dagpenger.kontrakter.felles.Fagsystem
import no.nav.dagpenger.kontrakter.felles.GeneriskIdSomString
import no.nav.dagpenger.kontrakter.felles.GeneriskIdSomUUID
import no.nav.dagpenger.kontrakter.felles.tilFagsystem
import no.nav.dagpenger.kontrakter.oppdrag.OppdragStatus
import no.nav.dagpenger.oppdrag.config.DatabaseConfiguration
import no.nav.dagpenger.oppdrag.iverksetting.mq.OppdragXmlMapper
import no.nav.dagpenger.oppdrag.iverksetting.tilstand.OppdragId
import no.nav.dagpenger.oppdrag.iverksetting.tilstand.OppdragLager
import no.nav.dagpenger.oppdrag.iverksetting.tilstand.OppdragLagerRepository
import no.nav.dagpenger.oppdrag.iverksetting.tilstand.tilGeneriskId
import no.nav.dagpenger.oppdrag.util.Containers
import no.nav.dagpenger.oppdrag.util.TestOppdragMedAvstemmingsdato
import no.nav.dagpenger.oppdrag.util.TestUtbetalingsoppdrag
import no.nav.dagpenger.oppdrag.util.somOppdragLager
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
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@JdbcTest
@ActiveProfiles("jdbc-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(
    initializers = [Containers.PostgresSQLInitializer::class],
    classes = [DatabaseConfiguration::class, OppdragLagerTestConfig::class],
)
@Testcontainers
internal class OppdragLagerRepositoryTest {
    @Autowired
    lateinit var oppdragLagerRepository: OppdragLagerRepository

    companion object {
        @Container
        var postgreSQLContainer = Containers.postgreSQLContainer
    }

    @Test
    fun skal_ikke_lagre_duplikat() {
        val oppdragLager = TestUtbetalingsoppdrag.utbetalingsoppdragMedTilfeldigAktoer().somOppdragLager

        oppdragLagerRepository.opprettOppdrag(oppdragLager)

        assertThrows<DuplicateKeyException> {
            oppdragLagerRepository.opprettOppdrag(oppdragLager)
        }
    }

    @Test
    fun skal_lagre_to_ulike_iverksettinger_samme_behandling() {
        val oppdragLager = TestUtbetalingsoppdrag.utbetalingsoppdragMedTilfeldigAktoer().somOppdragLager.copy(iverksettingId = "1")

        oppdragLagerRepository.opprettOppdrag(oppdragLager)

        assertDoesNotThrow {
            oppdragLagerRepository.opprettOppdrag(oppdragLager.copy(iverksettingId = "2"))
        }
    }

    @Test
    fun skal_lagre_status() {
        val oppdragLager =
            TestUtbetalingsoppdrag.utbetalingsoppdragMedTilfeldigAktoer().somOppdragLager
                .copy(status = OppdragStatus.LAGT_PÅ_KØ)

        oppdragLagerRepository.opprettOppdrag(oppdragLager)

        val hentetOppdrag = oppdragLagerRepository.hentOppdrag(oppdragLager.id)
        assertEquals(OppdragStatus.LAGT_PÅ_KØ, hentetOppdrag.status)

        oppdragLagerRepository.oppdaterStatus(hentetOppdrag.id, OppdragStatus.KVITTERT_OK)

        val hentetOppdatertOppdrag = oppdragLagerRepository.hentOppdrag(hentetOppdrag.id)
        assertEquals(OppdragStatus.KVITTERT_OK, hentetOppdatertOppdrag.status)
    }

    @Test
    fun skal_lagre_kvitteringsmelding() {
        val oppdragLager =
            TestUtbetalingsoppdrag.utbetalingsoppdragMedTilfeldigAktoer().somOppdragLager
                .copy(status = OppdragStatus.LAGT_PÅ_KØ)

        oppdragLagerRepository.opprettOppdrag(oppdragLager)
        val hentetOppdrag = oppdragLagerRepository.hentOppdrag(oppdragLager.id)
        val kvitteringsmelding = avvistKvitteringsmelding()

        oppdragLagerRepository.oppdaterKvitteringsmelding(hentetOppdrag.id, kvitteringsmelding)

        val hentetOppdatertOppdrag = oppdragLagerRepository.hentOppdrag(oppdragLager.id)
        assertTrue(kvitteringerErLike(kvitteringsmelding, hentetOppdatertOppdrag.kvitteringsmelding!!))
    }

    @Test
    fun skal_kun_hente_ut_ett_DP_oppdrag_for_grensesnittavstemming() {
        val dag = LocalDateTime.now()
        val startenPåDagen = dag.withHour(0).withMinute(0)
        val sluttenAvDagen = dag.withHour(23).withMinute(59)

        val baOppdragLager =
            TestOppdragMedAvstemmingsdato.lagTestUtbetalingsoppdrag(dag).somOppdragLager
        val baOppdragLager2 = TestOppdragMedAvstemmingsdato.lagTestUtbetalingsoppdrag(dag.minusDays(1)).somOppdragLager

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
}

private val OppdragLager.id: OppdragId
    get() {
        val behandlingId =
            Result.runCatching { UUID.fromString(this@id.behandlingId) }.fold(
                onSuccess = { GeneriskIdSomUUID(it) },
                onFailure = { GeneriskIdSomString(this.behandlingId) },
            )
        return OppdragId(
            fagsystem = this.fagsystem.tilFagsystem(),
            fagsakId = this.fagsakId.tilGeneriskId(),
            behandlingId = behandlingId,
            iverksettingId = this.iverksettingId,
        )
    }

private fun kvitteringerErLike(
    a: Mmel,
    b: Mmel,
): Boolean =
    a.systemId == b.systemId &&
        a.kodeMelding == b.kodeMelding &&
        a.alvorlighetsgrad == b.alvorlighetsgrad &&
        a.beskrMelding == b.beskrMelding &&
        a.sqlKode == b.sqlKode &&
        a.sqlState == b.sqlState &&
        a.sqlMelding == b.sqlMelding &&
        a.mqCompletionKode == b.mqCompletionKode &&
        a.mqReasonKode == b.mqReasonKode &&
        a.programId == b.programId &&
        a.sectionNavn == b.sectionNavn
