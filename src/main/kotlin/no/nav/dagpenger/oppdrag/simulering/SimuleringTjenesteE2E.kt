package no.nav.dagpenger.oppdrag.simulering

import no.nav.dagpenger.oppdrag.domene.DetaljertSimuleringResultat
import no.nav.dagpenger.oppdrag.domene.FeilutbetalingerFraSimulering
import no.nav.dagpenger.oppdrag.domene.HentFeilutbetalingerFraSimuleringRequest
import no.nav.dagpenger.oppdrag.domene.RestSimulerResultat
import no.nav.dagpenger.oppdrag.domene.Utbetalingsoppdrag
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningResponse
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.web.context.annotation.ApplicationScope

@Service
@ApplicationScope
@Profile("e2e")
class SimuleringTjenesteE2E : SimuleringTjeneste {

    override fun utførSimulering(utbetalingsoppdrag: Utbetalingsoppdrag): RestSimulerResultat = RestSimulerResultat(0)
    override fun utførSimuleringOghentDetaljertSimuleringResultat(utbetalingsoppdrag: Utbetalingsoppdrag): DetaljertSimuleringResultat =
        DetaljertSimuleringResultat(simuleringMottaker = emptyList())

    override fun hentSimulerBeregningResponse(utbetalingsoppdrag: Utbetalingsoppdrag): SimulerBeregningResponse =
        SimulerBeregningResponse()

    override fun hentFeilutbetalinger(request: HentFeilutbetalingerFraSimuleringRequest): FeilutbetalingerFraSimulering =
        FeilutbetalingerFraSimulering(feilutbetaltePerioder = emptyList())
}
