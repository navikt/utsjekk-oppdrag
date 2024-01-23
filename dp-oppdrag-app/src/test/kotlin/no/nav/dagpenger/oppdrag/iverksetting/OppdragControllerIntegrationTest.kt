package no.nav.dagpenger.oppdrag.iverksetting

import no.nav.dagpenger.kontrakter.oppdrag.OppdragId
import no.nav.dagpenger.kontrakter.oppdrag.OppdragStatus
import no.nav.dagpenger.kontrakter.oppdrag.Utbetalingsoppdrag
import no.nav.dagpenger.oppdrag.iverksetting.tilstand.OppdragLagerRepository
import no.nav.dagpenger.oppdrag.util.Containers
import no.nav.dagpenger.oppdrag.util.TestConfig
import no.nav.dagpenger.oppdrag.util.TestUtbetalingsoppdrag.utbetalingsoppdragMedTilfeldigAktoer
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
@ContextConfiguration(initializers = [Containers.PostgresSQLInitializer::class, Containers.MQInitializer::class])
@SpringBootTest(classes = [TestConfig::class])
@EnableJms
@Testcontainers
internal class OppdragControllerIntegrationTest {
    @Autowired lateinit var oppdragService: OppdragService

    @Autowired lateinit var oppdragLagerRepository: OppdragLagerRepository

    companion object {
        @Container var postgreSQLContainer = Containers.postgreSQLContainer

        @Container var ibmMQContainer = Containers.ibmMQContainer
    }

    @Test
    fun `Test skal lagre oppdrag for utbetalingoppdrag`() {
        val oppdragController = OppdragController(oppdragService)
        val utbetalingsoppdrag = utbetalingsoppdragMedTilfeldigAktoer()

        oppdragController.sendOppdrag(utbetalingsoppdrag)

        var oppdragStatus: OppdragStatus

        do {
            val oppdrag = oppdragLagerRepository.hentOppdrag(utbetalingsoppdrag.oppdragId)

            oppdragStatus = oppdrag.status
        } while (oppdragStatus == OppdragStatus.LAGT_PÅ_KØ)

        assertEquals(OppdragStatus.KVITTERT_OK, oppdragStatus)
    }

    @Test
    fun `Test skal returnere https statuscode 409 ved dobbel sending`() {
        val oppdragController = OppdragController(oppdragService)
        val utbetalingsoppdrag = utbetalingsoppdragMedTilfeldigAktoer()

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
    get() = OppdragId(fagsystem, aktør, utbetalingsperiode[0].behandlingId)
