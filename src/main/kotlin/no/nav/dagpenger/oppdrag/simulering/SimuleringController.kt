package no.nav.dagpenger.oppdrag.simulering

import io.swagger.v3.oas.annotations.Operation
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@ProtectedWithClaims(issuer = "azuread")
class SimuleringController(val simuleringService: SimuleringService) {

    @PostMapping(path = ["/simulering"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(hidden = true) // TODO: Gjør synlig i api-dok når endepunktet er klart til bruk
    fun postSimulering(@RequestBody request: SimuleringRequest): ResponseEntity<Simulering> {
        val simuleringsresponse = simuleringService.simuler(request)
        return ResponseEntity.ok(simuleringsresponse)
    }
}
