package no.nav.dagpenger.oppdrag.iverksetting

import no.nav.dagpenger.kontrakter.felles.Fagsystem
import no.nav.dagpenger.kontrakter.felles.tilFagsystem
import no.nav.dagpenger.kontrakter.oppdrag.OppdragId
import no.nav.dagpenger.oppdrag.config.DatabaseConfiguration
import no.nav.dagpenger.oppdrag.iverksetting.domene.OppdragStatus
import no.nav.dagpenger.oppdrag.iverksetting.mq.OppdragXmlMapper
import no.nav.dagpenger.oppdrag.iverksetting.tilstand.OppdragLager
import no.nav.dagpenger.oppdrag.iverksetting.tilstand.OppdragLagerRepository
import no.nav.dagpenger.oppdrag.util.Containers
import no.nav.dagpenger.oppdrag.util.TestOppdragMedAvstemmingsdato
import no.nav.dagpenger.oppdrag.util.TestUtbetalingsoppdrag
import no.nav.dagpenger.oppdrag.util.somOppdragLager
import org.junit.jupiter.api.Assertions
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
import java.util.UUID
import kotlin.test.assertFailsWith

@JdbcTest
@ActiveProfiles("jdbc-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(
    initializers = [Containers.PostgresSQLInitializer::class],
    classes = [DatabaseConfiguration::class, OppdragLagerTestConfig::class],
)
@Testcontainers
internal class OppdragLagerRepositoryJdbcTest {
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

        assertFailsWith<DuplicateKeyException> {
            oppdragLagerRepository.opprettOppdrag(oppdragLager)
        }
    }

    @Test
    fun skal_lagre_status() {
        val oppdragLager =
            TestUtbetalingsoppdrag.utbetalingsoppdragMedTilfeldigAktoer().somOppdragLager
                .copy(status = OppdragStatus.LAGT_PAA_KOE)

        oppdragLagerRepository.opprettOppdrag(oppdragLager)

        val hentetOppdrag = oppdragLagerRepository.hentOppdrag(oppdragLager.id)
        Assertions.assertEquals(OppdragStatus.LAGT_PAA_KOE, hentetOppdrag.status)

        oppdragLagerRepository.oppdaterStatus(hentetOppdrag.id, OppdragStatus.KVITTERT_OK)

        val hentetOppdatertOppdrag = oppdragLagerRepository.hentOppdrag(hentetOppdrag.id)
        Assertions.assertEquals(OppdragStatus.KVITTERT_OK, hentetOppdatertOppdrag.status)
    }

    @Test
    fun skal_lagre_kvitteringsmelding() {
        val oppdragLager =
            TestUtbetalingsoppdrag.utbetalingsoppdragMedTilfeldigAktoer().somOppdragLager
                .copy(status = OppdragStatus.LAGT_PAA_KOE)

        oppdragLagerRepository.opprettOppdrag(oppdragLager)
        val hentetOppdrag = oppdragLagerRepository.hentOppdrag(oppdragLager.id)
        val kvitteringsmelding = avvistKvitteringsmelding()

        oppdragLagerRepository.oppdaterKvitteringsmelding(hentetOppdrag.id, kvitteringsmelding)

        val hentetOppdatertOppdrag = oppdragLagerRepository.hentOppdrag(oppdragLager.id)
        org.assertj.core.api.Assertions.assertThat(
            kvitteringsmelding,
        ).isEqualToComparingFieldByField(hentetOppdatertOppdrag.kvitteringsmelding)
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
                Fagsystem.Dagpenger,
            )

        Assertions.assertEquals(1, oppdrageneTilGrensesnittavstemming.size)
        Assertions.assertEquals("DP", oppdrageneTilGrensesnittavstemming.first().fagsystem)
        Assertions.assertEquals(
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
    get() = OppdragId(this.fagsystem.tilFagsystem(), this.personIdent, UUID.fromString(this.behandlingId))
