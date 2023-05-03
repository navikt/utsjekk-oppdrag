package no.nav.dagpenger.oppdrag.rest

import no.nav.dagpenger.oppdrag.repository.SimuleringLagerTjenesteE2E
import no.nav.dagpenger.oppdrag.simulering.SimulerBeregningRequestMapper
import no.nav.dagpenger.oppdrag.simulering.SimuleringTjenesteImpl
import no.nav.dagpenger.oppdrag.simulering.mock.SimuleringSenderMock
import no.nav.dagpenger.oppdrag.simulering.util.lagTestUtbetalingsoppdragForFGBMedEttBarn
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertTrue

@ActiveProfiles("dev")
@SpringBootTest(
    classes = [SimuleringController::class, SimuleringSenderMock::class, SimuleringTjenesteImpl::class, SimulerBeregningRequestMapper::class, SimuleringLagerTjenesteE2E::class]
)
internal class SimuleringControllerIntegrationTest {

    @Autowired lateinit var simuleringController: SimuleringController

    @Test
    fun test_etterbetalingsbelop() {
        val response = simuleringController.hentEtterbetalingsbeløp(lagTestUtbetalingsoppdragForFGBMedEttBarn())
        assertTrue(1054 == response.body?.data?.etterbetaling || 3162 == response.body?.data?.etterbetaling)
    }
}
