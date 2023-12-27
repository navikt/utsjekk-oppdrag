package no.nav.dagpenger.oppdrag.simulering

import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.kontrakter.felles.Personident
import no.nav.system.os.eksponering.simulerfpservicewsbinding.SimulerFpService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)

class SimuleringControllerTest {

    private val simulerFpService: SimulerFpService = mockk()
    private val simuleringService = SimuleringService(simulerFpService)
    private val simuleringController = SimuleringController(simuleringService)

    @Test
    fun `post simulering`() {
        val requestBody = enSimuleringRequestBody()
        val request = SimuleringRequestBuilder(requestBody).build()

        every {
            simulerFpService.simulerBeregning(any())
        } answers {
            SimuleringGenerator().opprettSimuleringsResultat(request)
        }

        simuleringController.postSimulering(requestBody).also {
            assertEquals(200, it.statusCode.value())
        }
    }

    private fun enSimuleringRequestBody() = SimuleringRequestBody(
        fagområde = "TEST",
        fagsystemId = "FAGSYSTEM",
        fødselsnummer = Personident("15507600333"),
        mottaker = Personident("15507600333"),
        endringskode = Endringskode.NY,
        saksbehandler = "TEST",
        utbetalingsfrekvens = Utbetalingsfrekvens.UKENTLIG,
        utbetalingslinjer = listOf(
            Utbetalingslinje(
                delytelseId = "",
                endringskode = Endringskode.NY,
                klassekode = "",
                fom = LocalDate.of(2023, 1, 1),
                tom = LocalDate.of(2023, 1, 30),
                sats = 1000,
                grad = 100,
                refDelytelseId = null,
                refFagsystemId = null,
                datoStatusFom = null,
                statuskode = null,
                satstype = Satstype.MÅNED,
            )
        ),
    )
}
