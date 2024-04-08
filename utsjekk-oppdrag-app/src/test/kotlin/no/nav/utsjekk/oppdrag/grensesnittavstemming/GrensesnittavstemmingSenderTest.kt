package no.nav.utsjekk.oppdrag.grensesnittavstemming

import io.mockk.verify
import no.nav.utsjekk.kontrakter.felles.Fagsystem
import no.nav.utsjekk.oppdrag.MQInitializer
import no.nav.utsjekk.oppdrag.etUtbetalingsoppdrag
import no.nav.utsjekk.oppdrag.somOppdragLager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.jms.core.JmsTemplate
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDateTime

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(initializers = [MQInitializer::class])
class GrensesnittavstemmingSenderTest {
    private lateinit var jmsTemplate: JmsTemplate

    companion object {
        @Container
        val container = MQInitializer.container
    }

    @BeforeEach
    fun setup() {
        jmsTemplate = MQInitializer.getJmsTemplate()
    }

    @Test
    fun `skal sende grensesnittavstemming når påskrudd`() {
        val avstemmingSender = GrensesnittavstemmingSender(jmsTemplate, "true")

        avstemmingSender.sendGrensesnittAvstemming(avstemmingsdata()[0])

        verify(exactly = 1) { jmsTemplate.convertAndSend(any<String>(), any<String>()) }
    }

    private fun avstemmingsdata() =
        LocalDateTime.now().let { timestamp ->
            GrensesnittavstemmingMapper(
                oppdragsliste = listOf(etUtbetalingsoppdrag(timestamp).somOppdragLager),
                fagsystem = Fagsystem.DAGPENGER,
                fom = timestamp.minusDays(1),
                tom = timestamp,
            )
        }.lagAvstemmingsmeldinger()
}
