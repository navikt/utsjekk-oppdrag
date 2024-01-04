package no.nav.dagpenger.simulering.simulering

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@ProtectedWithClaims(issuer = "azuread")
@Tag(name = "Simulering")
class SimuleringController(val simuleringService: SimuleringService) {

    @PostMapping(path = ["/simulering"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(hidden = false) // TODO: Gjør synlig i api-dok når endepunktet er klart til bruk
    fun postSimulering(@RequestBody request: SimuleringRequestBody): ResponseEntity<Simulering> {
        val simuleringsresponse = simuleringService.simuler(request)
        return ResponseEntity.ok(simuleringsresponse)
    }
}
