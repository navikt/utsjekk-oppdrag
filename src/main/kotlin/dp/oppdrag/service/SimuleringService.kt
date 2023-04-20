package dp.oppdrag.service

import dp.oppdrag.model.*
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningResponse

interface SimuleringService {

    fun utfoerSimulering(utbetalingsoppdrag: Utbetalingsoppdrag): RestSimulerResultat
    fun utfoerSimuleringOgHentDetaljertSimuleringResultat(utbetalingsoppdrag: Utbetalingsoppdrag): DetaljertSimuleringResultat
    fun hentSimulerBeregningResponse(utbetalingsoppdrag: Utbetalingsoppdrag): SimulerBeregningResponse
    fun hentFeilutbetalinger(request: HentFeilutbetalingerFraSimuleringRequest): List<FeilutbetaltPeriode>
}
