package no.nav.dagpenger.oppdrag.iverksetting

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.kontrakter.felles.Fagsystem
import no.nav.dagpenger.kontrakter.felles.GeneriskIdSomUUID
import no.nav.dagpenger.kontrakter.oppdrag.Opphør
import no.nav.dagpenger.kontrakter.oppdrag.Utbetalingsoppdrag
import no.nav.dagpenger.kontrakter.oppdrag.Utbetalingsperiode
import no.nav.dagpenger.oppdrag.iverksetting.domene.OppdragStatus
import no.nav.dagpenger.oppdrag.iverksetting.mq.OppdragSender
import no.nav.dagpenger.oppdrag.iverksetting.tilstand.OppdragLager
import no.nav.dagpenger.oppdrag.iverksetting.tilstand.OppdragLagerRepository
import org.junit.jupiter.api.Test
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals

internal class OppdragControllerTest {
    private val localDateTimeNow = LocalDateTime.now()
    private val localDateNow = LocalDate.now()

    private val utbetalingsoppdrag =
        Utbetalingsoppdrag(
            kodeEndring = Utbetalingsoppdrag.KodeEndring.NY,
            fagSystem = Fagsystem.Dagpenger,
            saksnummer = GeneriskIdSomUUID(UUID.randomUUID()),
            aktør = "PERSONID",
            saksbehandlerId = "SAKSBEHANDLERID",
            utbetalingsperiode =
            listOf(
                Utbetalingsperiode(
                    true,
                    Opphør(localDateNow),
                    2,
                    1,
                    localDateNow,
                    "BATR",
                    localDateNow,
                    localDateNow,
                    BigDecimal.ONE,
                    Utbetalingsperiode.SatsType.MND,
                    "UTEBETALES_TIL",
                    GeneriskIdSomUUID(UUID.randomUUID()),
                ),
            ),
        )

    @Test
    fun `Skal lagre oppdrag for utbetalingoppdrag`() {
        val (oppdragLagerRepository, oppdragController) = mockkOppdragController(false)

        oppdragController.sendOppdrag(utbetalingsoppdrag)

        verify {
            oppdragLagerRepository.opprettOppdrag(
                match<OppdragLager> {
                    it.utgåendeOppdrag.contains("BA") &&
                        it.status == OppdragStatus.LAGT_PAA_KOE &&
                        it.opprettetTidspunkt > localDateTimeNow
                },
            )
        }
    }

    @Test
    fun `Skal kaste feil om oppdrag er lagret fra før`() {
        val (oppdragLagerRepository, oppdragController) = mockkOppdragController(true)

        val response = oppdragController.sendOppdrag(utbetalingsoppdrag)

        assertEquals(HttpStatus.CONFLICT, response.statusCode)

        verify(exactly = 1) { oppdragLagerRepository.opprettOppdrag(any()) }
    }

    private fun mockkOppdragController(alleredeOpprettet: Boolean = false): Pair<OppdragLagerRepository, OppdragController> {
        val oppdragSender = mockk<OppdragSender>(relaxed = true)

        val oppdragLagerRepository = mockk<OppdragLagerRepository>()
        if (alleredeOpprettet) {
            every { oppdragLagerRepository.opprettOppdrag(any()) } throws DuplicateKeyException("Duplicate key exception")
        } else {
            every { oppdragLagerRepository.opprettOppdrag(any()) } just Runs
        }

        val oppdragService = OppdragService(oppdragSender, oppdragLagerRepository)
        val oppdragController = OppdragController(oppdragService)

        return Pair(oppdragLagerRepository, oppdragController)
    }
}
