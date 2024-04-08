package no.nav.utsjekk.oppdrag.iverksetting

import no.nav.utsjekk.kontrakter.oppdrag.OppdragStatus
import no.nav.utsjekk.kontrakter.oppdrag.Utbetalingsoppdrag
import no.nav.utsjekk.oppdrag.MQInitializer
import no.nav.utsjekk.oppdrag.PostgreSQLInitializer
import no.nav.utsjekk.oppdrag.etUtbetalingsoppdrag
import no.nav.utsjekk.oppdrag.iverksetting.tilstand.OppdragId
import no.nav.utsjekk.oppdrag.iverksetting.tilstand.OppdragLagerRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.jms.annotation.EnableJms
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals

@ActiveProfiles("local")
@ContextConfiguration(initializers = [PostgreSQLInitializer::class, MQInitializer::class])
@SpringBootTest
@EnableJms
@Testcontainers
internal class OppdragControllerIntegrationTest {
    @Autowired
    lateinit var oppdragService: OppdragService

    @Autowired
    lateinit var oppdragLagerRepository: OppdragLagerRepository

    companion object {
        @Container
        var postgreSQLContainer = PostgreSQLInitializer.container

        @Container
        var ibmMQContainer = MQInitializer.container
    }

    @Test
    fun `skal lagre oppdrag for utbetalingoppdrag`() {
        val oppdragController = OppdragController(oppdragService)
        val utbetalingsoppdrag = etUtbetalingsoppdrag()

        oppdragController.sendOppdrag(utbetalingsoppdrag)

        var oppdragStatus: OppdragStatus

        do {
            val oppdrag = oppdragLagerRepository.hentOppdrag(utbetalingsoppdrag.oppdragId)

            oppdragStatus = oppdrag.status
        } while (oppdragStatus == OppdragStatus.LAGT_PÅ_KØ)

        assertEquals(OppdragStatus.KVITTERT_OK, oppdragStatus)
    }

    @Test
    fun `skal returnere https statuscode 409 ved dobbel sending`() {
        val oppdragController = OppdragController(oppdragService)
        val utbetalingsoppdrag = etUtbetalingsoppdrag()

        oppdragController.sendOppdrag(utbetalingsoppdrag).also {
            assertEquals(HttpStatus.CREATED, it.statusCode)
        }

        oppdragController.sendOppdrag(utbetalingsoppdrag).also {
            assertEquals(HttpStatus.CONFLICT, it.statusCode)
        }

        var oppdragStatus: OppdragStatus

        do {
            val oppdrag = oppdragLagerRepository.hentOppdrag(utbetalingsoppdrag.oppdragId)

            oppdragStatus = oppdrag.status
        } while (oppdragStatus == OppdragStatus.LAGT_PÅ_KØ)

        assertEquals(OppdragStatus.KVITTERT_OK, oppdragStatus)
    }
}

private val Utbetalingsoppdrag.oppdragId
    get() =
        OppdragId(
            fagsystem = this.fagsystem,
            fagsakId = this.saksnummer,
            behandlingId = this.utbetalingsperiode[0].behandlingId,
            iverksettingId = this.iverksettingId,
        )
