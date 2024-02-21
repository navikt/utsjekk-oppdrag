package no.nav.dagpenger.oppdrag.iverksetting

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import jakarta.jms.TextMessage
import no.nav.dagpenger.kontrakter.oppdrag.OppdragStatus
import no.nav.dagpenger.kontrakter.oppdrag.Utbetalingsoppdrag
import no.nav.dagpenger.oppdrag.iverksetting.domene.Kvitteringstatus
import no.nav.dagpenger.oppdrag.iverksetting.domene.OppdragMapper
import no.nav.dagpenger.oppdrag.iverksetting.domene.kvitteringstatus
import no.nav.dagpenger.oppdrag.iverksetting.mq.OppdragMottaker
import no.nav.dagpenger.oppdrag.iverksetting.tilstand.OppdragLager
import no.nav.dagpenger.oppdrag.iverksetting.tilstand.OppdragLagerRepository
import no.nav.dagpenger.oppdrag.iverksetting.tilstand.id
import no.nav.dagpenger.oppdrag.util.TestUtbetalingsoppdrag.utbetalingsoppdragMedTilfeldigAktoer
import no.nav.dagpenger.oppdrag.util.somOppdragLager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.core.env.Environment
import kotlin.test.assertEquals

class OppdragMQMottakTest {
    private lateinit var oppdragMottaker: OppdragMottaker

    private val localEnv: Environment
        get() =
            mockk<Environment>().apply {
                every { activeProfiles } returns arrayOf("local")
            }

    @BeforeEach
    fun setUp() {
        oppdragMottaker = OppdragMottaker(mockk(), localEnv)
    }

    @Test
    fun skal_tolke_kvittering_riktig_ved_OK() {
        val kvittering = lesKvittering("kvittering-akseptert.xml")
        val statusFraKvittering = oppdragMottaker.lesKvittering(kvittering).kvitteringstatus

        assertEquals(Kvitteringstatus.OK, statusFraKvittering)
    }

    @Test
    fun skal_tolke_kvittering_riktig_ved_feil() {
        val kvittering: String = lesKvittering("kvittering-avvist.xml")
        val statusFraKvittering = oppdragMottaker.lesKvittering(kvittering).kvitteringstatus

        assertEquals(Kvitteringstatus.AVVIST_FUNKSJONELLE_FEIL, statusFraKvittering)
    }

    @Test
    fun skal_lagre_status_og_mmel_fra_kvittering() {
        val oppdragLager = utbetalingsoppdragMedTilfeldigAktoer().somOppdragLager

        val oppdragLagerRepository = mockk<OppdragLagerRepository>()

        every { oppdragLagerRepository.hentAlleVersjonerAvOppdrag(any()) } returns
            listOf(oppdragLager)

        every { oppdragLagerRepository.oppdaterStatus(any(), any()) } just Runs
        every { oppdragLagerRepository.oppdaterKvitteringsmelding(any(), any()) } just Runs

        val oppdragMottaker = OppdragMottaker(oppdragLagerRepository, localEnv)

        oppdragMottaker.mottaKvitteringFraOppdrag("kvittering-akseptert.xml".fraRessursSomTextMessage)

        verify(exactly = 1) { oppdragLagerRepository.hentAlleVersjonerAvOppdrag(any()) }
        verify(exactly = 1) { oppdragLagerRepository.oppdaterStatus(any(), any()) }
        verify(exactly = 1) { oppdragLagerRepository.oppdaterKvitteringsmelding(any(), any()) }
    }

    @Test
    fun skal_lagre_kvittering_på_riktig_versjon() {
        val oppdragLager = utbetalingsoppdragMedTilfeldigAktoer().somOppdragLager.apply { status = OppdragStatus.KVITTERT_OK }
        val oppdragLagerV1 = utbetalingsoppdragMedTilfeldigAktoer().somOppdragLagerMedVersjon(1)

        val oppdragLagerRepository = mockk<OppdragLagerRepository>()

        every { oppdragLagerRepository.hentAlleVersjonerAvOppdrag(any()) } returns
            listOf(oppdragLager, oppdragLagerV1)

        every { oppdragLagerRepository.oppdaterStatus(any(), any(), any()) } just Runs
        every { oppdragLagerRepository.oppdaterKvitteringsmelding(any(), any(), any()) } just Runs

        val oppdragMottaker = OppdragMottaker(oppdragLagerRepository, localEnv)

        oppdragMottaker.mottaKvitteringFraOppdrag("kvittering-akseptert.xml".fraRessursSomTextMessage)

        verify(exactly = 0) { oppdragLagerRepository.oppdaterStatus(any(), any(), 0) }
        verify(exactly = 1) { oppdragLagerRepository.oppdaterStatus(any(), any(), 1) }
        verify(exactly = 0) { oppdragLagerRepository.oppdaterKvitteringsmelding(any(), any(), 0) }
        verify(exactly = 1) { oppdragLagerRepository.oppdaterKvitteringsmelding(any(), any(), 1) }
    }

    @Test
    fun skal_lagre_kvittering_på_riktig_iverksetting_ved_flere_iverksettinger() {
        val iverksettingId1 = "1"
        val iverksettingId2 = "2"
        val oppdragLager1 =
            utbetalingsoppdragMedTilfeldigAktoer(
                iverksettingId1,
            ).somOppdragLager.apply { status = OppdragStatus.KVITTERT_OK }
        val oppdragLager2 =
            utbetalingsoppdragMedTilfeldigAktoer(
                iverksettingId2,
            ).somOppdragLager.apply { status = OppdragStatus.LAGT_PÅ_KØ }

        val oppdragLagerRepository = mockk<OppdragLagerRepository>()

        every { oppdragLagerRepository.hentAlleVersjonerAvOppdrag(any()) } returns
            listOf(oppdragLager1, oppdragLager2)

        every { oppdragLagerRepository.oppdaterStatus(any(), any(), any()) } just Runs
        every { oppdragLagerRepository.oppdaterKvitteringsmelding(any(), any(), any()) } just Runs

        val oppdragMottaker = OppdragMottaker(oppdragLagerRepository, localEnv)

        oppdragMottaker.mottaKvitteringFraOppdrag("kvittering-akseptert.xml".fraRessursSomTextMessage)

        verify(exactly = 0) { oppdragLagerRepository.oppdaterStatus(oppdragLager1.id, any(), any()) }
        verify(exactly = 1) { oppdragLagerRepository.oppdaterStatus(oppdragLager2.id, any(), any()) }
        verify(exactly = 0) { oppdragLagerRepository.oppdaterKvitteringsmelding(oppdragLager1.id, any(), any()) }
        verify(exactly = 1) { oppdragLagerRepository.oppdaterKvitteringsmelding(oppdragLager2.id, any(), any()) }
    }

    @Test
    fun skal_logge_error_hvis_det_finnes_to_identiske_oppdrag_i_databasen() {
        val oppdragLagerRepository = mockk<OppdragLagerRepository>()

        every { oppdragLagerRepository.hentAlleVersjonerAvOppdrag(any()) } throws Exception()

        every { oppdragLagerRepository.opprettOppdrag(any()) } just Runs

        val oppdragMottaker = OppdragMottaker(oppdragLagerRepository, localEnv)

        assertThrows<Exception> { oppdragMottaker.mottaKvitteringFraOppdrag("kvittering-akseptert.xml".fraRessursSomTextMessage) }
        verify(exactly = 0) { oppdragLagerRepository.opprettOppdrag(any<OppdragLager>()) }
    }

    @Test
    fun skal_logge_error_hvis_oppdraget_mangler_i_databasen() {
        val oppdragLagerRepository = mockk<OppdragLagerRepository>()

        every { oppdragLagerRepository.hentAlleVersjonerAvOppdrag(any()) } throws Exception()
        every { oppdragLagerRepository.opprettOppdrag(any()) } just Runs

        val oppdragMottaker = OppdragMottaker(oppdragLagerRepository, localEnv)

        assertThrows<Exception> { oppdragMottaker.mottaKvitteringFraOppdrag("kvittering-akseptert.xml".fraRessursSomTextMessage) }
        verify(exactly = 0) { oppdragLagerRepository.opprettOppdrag(any<OppdragLager>()) }
    }

    @Test
    fun skal_logge_warn_hvis_oppdrag_i_databasen_har_uventet_status() {
        val oppdragLager = utbetalingsoppdragMedTilfeldigAktoer().somOppdragLager

        val oppdragLagerRepository = mockk<OppdragLagerRepository>()

        every { oppdragLagerRepository.hentAlleVersjonerAvOppdrag(any()) } returns
            listOf(oppdragLager.copy(status = OppdragStatus.KVITTERT_OK))

        every { oppdragLagerRepository.oppdaterStatus(any(), OppdragStatus.KVITTERT_OK) } just Runs
        every { oppdragLagerRepository.oppdaterKvitteringsmelding(any(), any()) } just Runs

        val oppdragMottaker = OppdragMottaker(oppdragLagerRepository, localEnv)

        oppdragMottaker.mottaKvitteringFraOppdrag("kvittering-akseptert.xml".fraRessursSomTextMessage)

        verify(exactly = 1) { oppdragLagerRepository.hentAlleVersjonerAvOppdrag(any()) }
    }

    private fun lesKvittering(filnavn: String) =
        this::class.java.getResourceAsStream("/$filnavn")?.bufferedReader().use { it?.readText() ?: "" }

    private val String.fraRessursSomTextMessage: TextMessage
        get() =
            mockk<TextMessage>().apply {
                every<String?> { text } returns lesKvittering(this@fraRessursSomTextMessage)
            }
}

internal fun Utbetalingsoppdrag.somOppdragLagerMedVersjon(versjon: Int): OppdragLager {
    val tilOppdrag110 = OppdragMapper.tilOppdrag110(this)
    val oppdrag = OppdragMapper.tilOppdrag(tilOppdrag110)

    return OppdragLager.lagFraOppdrag(this, oppdrag, versjon)
}
