package no.nav.dagpenger.oppdrag.simulering

import no.nav.dagpenger.oppdrag.domene.DetaljertSimuleringResultat
import no.nav.dagpenger.oppdrag.domene.FeilutbetalingerFraSimulering
import no.nav.dagpenger.oppdrag.domene.HentFeilutbetalingerFraSimuleringRequest
import no.nav.dagpenger.oppdrag.domene.RestSimulerResultat
import no.nav.dagpenger.oppdrag.domene.Utbetalingsoppdrag
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningResponse

interface SimuleringTjeneste {

    fun utførSimulering(utbetalingsoppdrag: Utbetalingsoppdrag): RestSimulerResultat
    fun utførSimuleringOghentDetaljertSimuleringResultat(utbetalingsoppdrag: Utbetalingsoppdrag): DetaljertSimuleringResultat
    fun hentSimulerBeregningResponse(utbetalingsoppdrag: Utbetalingsoppdrag): SimulerBeregningResponse
    fun hentFeilutbetalinger(request: HentFeilutbetalingerFraSimuleringRequest): FeilutbetalingerFraSimulering
}
