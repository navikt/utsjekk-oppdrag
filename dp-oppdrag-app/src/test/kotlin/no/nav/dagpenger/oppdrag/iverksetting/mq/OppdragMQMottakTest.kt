package no.nav.dagpenger.oppdrag.iverksetting.mq

import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import jakarta.jms.TextMessage
import no.nav.dagpenger.kontrakter.oppdrag.OppdragStatus
import no.nav.dagpenger.kontrakter.oppdrag.Utbetalingsoppdrag
import no.nav.dagpenger.oppdrag.etUtbetalingsoppdrag
import no.nav.dagpenger.oppdrag.iverksetting.domene.Kvitteringstatus
import no.nav.dagpenger.oppdrag.iverksetting.domene.OppdragMapper
import no.nav.dagpenger.oppdrag.iverksetting.domene.kvitteringstatus
import no.nav.dagpenger.oppdrag.iverksetting.tilstand.OppdragLager
import no.nav.dagpenger.oppdrag.iverksetting.tilstand.OppdragLagerRepository
import no.nav.dagpenger.oppdrag.iverksetting.tilstand.dekomprimertId
import no.nav.dagpenger.oppdrag.iverksetting.tilstand.id
import no.nav.dagpenger.oppdrag.somOppdragLager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.core.env.Environment
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OppdragMQMottakTest {
    private lateinit var oppdragMottaker: OppdragMottaker

    private val oppdragLagerRepository = mockk<OppdragLagerRepository>()

    private val localEnv: Environment
        get() =
            mockk<Environment>().apply {
                every { activeProfiles } returns arrayOf("local")
            }

    @BeforeAll
    fun setup() {
        oppdragMottaker = OppdragMottaker(oppdragLagerRepository, localEnv)
    }

    @AfterEach
    fun cleanup() {
        clearMocks(oppdragLagerRepository)
    }

    @Test
    fun `skal tolke kvittering riktig ved ok`() {
        val kvittering = lesKvittering("kvittering-akseptert.xml")
        val statusFraKvittering = OppdragXmlMapper.tilOppdrag(kvittering).kvitteringstatus

        assertEquals(Kvitteringstatus.OK, statusFraKvittering)
    }

    @Test
    fun `skal deserialisere kvittering som feilet i testmiljø`() {
        val kvittering = "kvittering-test.xml".fraRessursSomTextMessage
        val deserialisert = OppdragXmlMapper.tilOppdrag(oppdragMottaker.leggTilNamespacePrefiks(kvittering.text))
        assertDoesNotThrow { deserialisert.dekomprimertId }
    }

    @Test
    fun `skal tolke kvittering riktig ved feil`() {
        val kvittering = lesKvittering("kvittering-avvist.xml")
        val statusFraKvittering = OppdragXmlMapper.tilOppdrag(kvittering).kvitteringstatus

        assertEquals(Kvitteringstatus.AVVIST_FUNKSJONELLE_FEIL, statusFraKvittering)
    }

    @Test
    fun `skal lagre status og mmel fra kvittering`() {
        val oppdragLager = etUtbetalingsoppdrag().somOppdragLager

        every { oppdragLagerRepository.hentAlleVersjonerAvOppdrag(any()) } returns listOf(oppdragLager)
        every { oppdragLagerRepository.oppdaterStatus(any(), any()) } just Runs
        every { oppdragLagerRepository.oppdaterKvitteringsmelding(any(), any()) } just Runs

        oppdragMottaker.mottaKvitteringFraOppdrag("kvittering-akseptert.xml".fraRessursSomTextMessage)

        verify(exactly = 1) { oppdragLagerRepository.hentAlleVersjonerAvOppdrag(any()) }
        verify(exactly = 1) { oppdragLagerRepository.oppdaterStatus(any(), any()) }
        verify(exactly = 1) { oppdragLagerRepository.oppdaterKvitteringsmelding(any(), any()) }
    }

    @Test
    fun `skal lagre kvittering på riktig versjon`() {
        val oppdragLager = etUtbetalingsoppdrag().somOppdragLager.apply { status = OppdragStatus.KVITTERT_OK }
        val oppdragLagerV1 = etUtbetalingsoppdrag().somOppdragLagerMedVersjon(1)

        every { oppdragLagerRepository.hentAlleVersjonerAvOppdrag(any()) } returns
            listOf(oppdragLager, oppdragLagerV1)

        every { oppdragLagerRepository.oppdaterStatus(any(), any(), any()) } just Runs
        every { oppdragLagerRepository.oppdaterKvitteringsmelding(any(), any(), any()) } just Runs

        oppdragMottaker.mottaKvitteringFraOppdrag("kvittering-akseptert.xml".fraRessursSomTextMessage)

        verify(exactly = 0) { oppdragLagerRepository.oppdaterStatus(any(), any(), 0) }
        verify(exactly = 1) { oppdragLagerRepository.oppdaterStatus(any(), any(), 1) }
        verify(exactly = 0) { oppdragLagerRepository.oppdaterKvitteringsmelding(any(), any(), 0) }
        verify(exactly = 1) { oppdragLagerRepository.oppdaterKvitteringsmelding(any(), any(), 1) }
    }

    @Test
    fun `skal lagre kvittering på riktig iverksetting ved flere iverksettinger`() {
        val oppdragLager1 =
            etUtbetalingsoppdrag(iverksettingId = "1").somOppdragLager.apply {
                status = OppdragStatus.KVITTERT_OK
            }
        val oppdragLager2 =
            etUtbetalingsoppdrag(iverksettingId = "2").somOppdragLager.apply {
                status = OppdragStatus.LAGT_PÅ_KØ
            }

        every { oppdragLagerRepository.hentAlleVersjonerAvOppdrag(any()) } returns
            listOf(oppdragLager1, oppdragLager2)

        every { oppdragLagerRepository.oppdaterStatus(any(), any(), any()) } just Runs
        every { oppdragLagerRepository.oppdaterKvitteringsmelding(any(), any(), any()) } just Runs

        oppdragMottaker.mottaKvitteringFraOppdrag("kvittering-akseptert.xml".fraRessursSomTextMessage)

        verify(exactly = 0) { oppdragLagerRepository.oppdaterStatus(oppdragLager1.id, any(), any()) }
        verify(exactly = 1) { oppdragLagerRepository.oppdaterStatus(oppdragLager2.id, any(), any()) }
        verify(exactly = 0) { oppdragLagerRepository.oppdaterKvitteringsmelding(oppdragLager1.id, any(), any()) }
        verify(exactly = 1) { oppdragLagerRepository.oppdaterKvitteringsmelding(oppdragLager2.id, any(), any()) }
    }

    @Test
    fun `oppretter ikke oppdrag hvis henting av oppdrag feiler`() {
        every { oppdragLagerRepository.hentAlleVersjonerAvOppdrag(any()) } throws Exception()

        assertThrows<Exception> { oppdragMottaker.mottaKvitteringFraOppdrag("kvittering-akseptert.xml".fraRessursSomTextMessage) }
        verify(exactly = 0) { oppdragLagerRepository.opprettOppdrag(any<OppdragLager>()) }
    }

    @Test
    fun `skal logge warn hvis oppdrag i databasen har uventet status`() {
        val oppdragLager = etUtbetalingsoppdrag().somOppdragLager

        every { oppdragLagerRepository.hentAlleVersjonerAvOppdrag(any()) } returns
            listOf(oppdragLager.copy(status = OppdragStatus.KVITTERT_OK))
        every { oppdragLagerRepository.oppdaterStatus(any(), OppdragStatus.KVITTERT_OK) } just Runs
        every { oppdragLagerRepository.oppdaterKvitteringsmelding(any(), any()) } just Runs

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

    private fun Utbetalingsoppdrag.somOppdragLagerMedVersjon(versjon: Int): OppdragLager {
        val tilOppdrag110 = OppdragMapper.tilOppdrag110(this)
        val oppdrag = OppdragMapper.tilOppdrag(tilOppdrag110)

        return OppdragLager.lagFraOppdrag(this, oppdrag, versjon)
    }
}
