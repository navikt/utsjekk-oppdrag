package dp.oppdrag.listener

import dp.oppdrag.defaultXmlMapper
import dp.oppdrag.model.*
import dp.oppdrag.repository.OppdragLagerRepository
import io.mockk.*
import no.trygdeetaten.skjema.oppdrag.Mmel
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import no.trygdeetaten.skjema.oppdrag.Oppdrag110
import no.trygdeetaten.skjema.oppdrag.OppdragsLinje150
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.jms.TextMessage
import kotlin.test.Test
import kotlin.test.assertEquals

class OppdragListenerMQTest {

    @Test
    fun shouldProcessMessageOk() {
        shouldProcessMessage(OppdragStatus.OK, OppdragLagerStatus.KVITTERT_OK)
    }

    @Test
    fun shouldProcessMessageMedMangler() {
        shouldProcessMessage(OppdragStatus.AKSEPTERT_MEN_NOE_ER_FEIL, OppdragLagerStatus.KVITTERT_MED_MANGLER)
    }

    @Test
    fun shouldProcessMessageFunksjonellFeil() {
        shouldProcessMessage(OppdragStatus.AVVIST_FUNKSJONELLE_FEIL, OppdragLagerStatus.KVITTERT_FUNKSJONELL_FEIL)
    }

    @Test
    fun shouldProcessMessageTekniskFeil() {
        shouldProcessMessage(OppdragStatus.AVVIST_TEKNISK_FEIL, OppdragLagerStatus.KVITTERT_TEKNISK_FEIL)
    }

    @Test
    fun shouldProcessMessageUkjent() {
        shouldProcessMessage(OppdragStatus.UKJENT, OppdragLagerStatus.KVITTERT_UKJENT)
    }

    @Test
    fun shouldNotThrowExceptionWhenCantFindOppdrag() {
        shouldProcessMessage(OppdragStatus.OK, OppdragLagerStatus.KVITTERT_OK, OppdragLagerStatus.KVITTERT_OK, 0)
    }

    @Test
    fun shouldNotThrowExceptionWhenSomethingWrong() {
        shouldProcessMessage(OppdragStatus.OK, OppdragLagerStatus.KVITTERT_OK, OppdragLagerStatus.LAGT_PAA_KOE, 1, true)
    }

    private fun shouldProcessMessage(
        oppdragStatus: OppdragStatus,
        oppdragLagerStatus: OppdragLagerStatus,
        existingOppdragLagerStatus: OppdragLagerStatus = OppdragLagerStatus.LAGT_PAA_KOE,
        timesCalledRepository: Int = 1,
        shouldThrowException: Boolean = false
    ) {
        // Testdata
        val oppdrag = Oppdrag()
            .withOppdrag110(
                Oppdrag110()
                    .withKodeFagomraade("KodeFagomraade")
                    .withOppdragGjelderId("123456789")
                    .withOppdragsLinje150(
                        OppdragsLinje150().withHenvisning("Henvisning")
                    )
            )
            .withMmel(
                Mmel().withAlvorlighetsgrad(oppdragStatus.kode)
            )

        val oppdragLager = OppdragLager(
            uuid = UUID.randomUUID(),
            fagsystem = "Fagsystem",
            personIdent = "01020312345",
            fagsakId = "1",
            behandlingId = "2",
            utbetalingsoppdrag = Utbetalingsoppdrag(
                kodeEndring = Utbetalingsoppdrag.KodeEndring.NY,
                fagSystem = "EFOG",
                saksnummer = "12345",
                aktoer = "01020312345",
                saksbehandlerId = "S123456",
                avstemmingTidspunkt = LocalDateTime.now(),
                utbetalingsperiode = listOf(
                    Utbetalingsperiode(
                        erEndringPaaEksisterendePeriode = false,
                        opphoer = null,
                        periodeId = 2L,
                        forrigePeriodeId = 1L,
                        datoForVedtak = LocalDate.now(),
                        klassifisering = "",
                        vedtakdatoFom = LocalDate.now(),
                        vedtakdatoTom = LocalDate.now(),
                        sats = BigDecimal.TEN,
                        satsType = Utbetalingsperiode.SatsType.DAG,
                        utbetalesTil = "",
                        behandlingId = 3L,
                        utbetalingsgrad = 100
                    )
                ),
                gOmregning = false
            ),
            utgaaendeOppdrag = "Utgående oppdrag",
            status = existingOppdragLagerStatus,
            avstemmingTidspunkt = LocalDateTime.now(),
            opprettetTidspunkt = LocalDateTime.now(),
            kvitteringsmelding = null,
            versjon = 0
        )

        // Mock
        val oppdragLagerRepository = mockk<OppdragLagerRepository>()
        every { oppdragLagerRepository.hentAlleVersjonerAvOppdrag(any()) } returns listOf(oppdragLager)
        every { oppdragLagerRepository.oppdaterKvitteringsmelding(any(), any(), any()) } just Runs
        if (shouldThrowException) {
            every { oppdragLagerRepository.oppdaterStatus(any(), any(), any()) } throws Exception()
        } else {
            every { oppdragLagerRepository.oppdaterStatus(any(), any(), any()) } just Runs
        }

        val message = mockk<TextMessage>()
        every { message.text } returns defaultXmlMapper.writeValueAsString(oppdrag)

        // Run
        val oppdragListenerMQ = OppdragListenerMQ(oppdragLagerRepository)
        oppdragListenerMQ.onMessage(message)

        // Check
        val oppdragId = OppdragId(
            oppdrag.oppdrag110.kodeFagomraade,
            oppdrag.oppdrag110.oppdragGjelderId,
            oppdrag.oppdrag110.oppdragsLinje150?.get(0)?.henvisning!!
        )
        verify(exactly = timesCalledRepository) {
            oppdragLagerRepository.oppdaterKvitteringsmelding(
                oppdragId,
                // We use this approach because Mmel is a Java object, not a Kotlin object
                withArg {
                    assertEquals(
                        oppdrag.mmel.alvorlighetsgrad,
                        it.alvorlighetsgrad
                    )
                },
                0
            )
        }
        verify(exactly = timesCalledRepository) {
            oppdragLagerRepository.oppdaterStatus(
                oppdragId,
                oppdragLagerStatus,
                0
            )
        }
    }
}