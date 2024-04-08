package no.nav.utsjekk.simulering.simulering

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.utsjekk.simulering.simulering.dto.SimuleringRequestBody
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
    fun postSimulering(
        @RequestBody request: SimuleringRequestBody,
    ) = ResponseEntity.ok(simuleringService.simuler(request))
}
