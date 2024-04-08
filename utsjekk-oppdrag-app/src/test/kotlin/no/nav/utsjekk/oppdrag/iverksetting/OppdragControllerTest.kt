package no.nav.utsjekk.oppdrag.iverksetting

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.utsjekk.kontrakter.oppdrag.OppdragStatus
import no.nav.utsjekk.oppdrag.etUtbetalingsoppdrag
import no.nav.utsjekk.oppdrag.iverksetting.mq.OppdragSender
import no.nav.utsjekk.oppdrag.iverksetting.tilstand.OppdragLager
import no.nav.utsjekk.oppdrag.iverksetting.tilstand.OppdragLagerRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import java.time.LocalDateTime

internal class OppdragControllerTest {
    private val localDateTimeNow = LocalDateTime.now()
    private val utbetalingsoppdrag = etUtbetalingsoppdrag(stønadstype = "BATR")

    @Test
    fun `skal lagre oppdrag for utbetalingoppdrag`() {
        val (oppdragLagerRepository, oppdragController) = mockkOppdragController(false)

        oppdragController.sendOppdrag(utbetalingsoppdrag)

        verify {
            oppdragLagerRepository.opprettOppdrag(
                match<OppdragLager> {
                    it.utgåendeOppdrag.contains("BA") &&
                        it.status == OppdragStatus.LAGT_PÅ_KØ &&
                        it.opprettetTidspunkt > localDateTimeNow
                },
            )
        }
    }

    @Test
    fun `skal kaste feil om oppdrag er lagret fra før`() {
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
