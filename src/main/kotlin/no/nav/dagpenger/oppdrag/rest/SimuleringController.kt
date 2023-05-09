package no.nav.dagpenger.oppdrag.rest

import no.nav.dagpenger.oppdrag.common.Ressurs
import no.nav.dagpenger.oppdrag.common.RessursUtils.ok
import no.nav.dagpenger.oppdrag.domene.DetaljertSimuleringResultat
import no.nav.dagpenger.oppdrag.domene.FeilutbetalingerFraSimulering
import no.nav.dagpenger.oppdrag.domene.HentFeilutbetalingerFraSimuleringRequest
import no.nav.dagpenger.oppdrag.domene.RestSimulerResultat
import no.nav.dagpenger.oppdrag.domene.Utbetalingsoppdrag
import no.nav.dagpenger.oppdrag.simulering.SimuleringTjeneste
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
@RequestMapping("/simulering", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
class SimuleringController(@Autowired val simuleringTjeneste: SimuleringTjeneste) {

    val logger: Logger = LoggerFactory.getLogger(SimuleringController::class.java)

    @PostMapping(path = ["/etterbetalingsbelop"])
    fun hentEtterbetalingsbeløp(
        @Valid @RequestBody
        utbetalingsoppdrag: Utbetalingsoppdrag
    ): ResponseEntity<Ressurs<RestSimulerResultat>> {
        logger.info("Hente simulert etterbetaling for saksnr ${utbetalingsoppdrag.saksnummer}")
        return ok(simuleringTjeneste.utførSimulering(utbetalingsoppdrag))
    }

    @PostMapping(path = ["/v1"])
    fun utførSimuleringOgHentResultat(
        @Valid @RequestBody
        utbetalingsoppdrag: Utbetalingsoppdrag
    ): ResponseEntity<Ressurs<DetaljertSimuleringResultat>> {
        return ok(simuleringTjeneste.utførSimuleringOghentDetaljertSimuleringResultat(utbetalingsoppdrag))
    }

    // Temporær funksjon som skal brukes for å teste responser fra oppdrag.
    // TODO: skal fjernes når den ikke mer er i bruk.
    @PostMapping(path = ["/direktesimulering"])
    fun direkteSimulering(
        @Valid @RequestBody
        utbetalingsoppdrag: Utbetalingsoppdrag
    ): ResponseEntity<Ressurs<SimulerBeregningResponse>> =
        ok(simuleringTjeneste.hentSimulerBeregningResponse(utbetalingsoppdrag))

    @PostMapping(path = ["/feilutbetalinger"])
    fun hentFeilutbetalinger(@Valid @RequestBody request: HentFeilutbetalingerFraSimuleringRequest): ResponseEntity<Ressurs<FeilutbetalingerFraSimulering>> {
        logger.info(
            "Henter feilutbetalinger for ytelsestype=${request.ytelsestype}, " +
                "fagsak=${request.eksternFagsakId}," +
                " behandlingId=${request.eksternFagsakId}"
        )
        return ok(simuleringTjeneste.hentFeilutbetalinger(request))
    }
}