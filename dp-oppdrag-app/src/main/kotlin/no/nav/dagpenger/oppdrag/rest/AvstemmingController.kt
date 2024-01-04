package no.nav.dagpenger.oppdrag.rest

import no.nav.dagpenger.kontrakter.oppdrag.GrensesnittavstemmingRequest
import no.nav.dagpenger.oppdrag.common.Ressurs
import no.nav.dagpenger.oppdrag.common.RessursUtils.illegalState
import no.nav.dagpenger.oppdrag.common.RessursUtils.ok
import no.nav.dagpenger.oppdrag.service.GrensesnittavstemmingService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@ProtectedWithClaims(issuer = "azuread")
@Validated
class AvstemmingController(
    @Autowired val grensesnittavstemmingService: GrensesnittavstemmingService
) {

    @PostMapping(path = ["/grensesnittavstemming"])
    fun grensesnittavstemming(@RequestBody request: GrensesnittavstemmingRequest): ResponseEntity<Ressurs<String>> {
        LOG.info("Grensesnittavstemming: Kjører for ${request.fagsystem}-oppdrag fra ${request.fra} til ${request.til}")

        return Result.runCatching { grensesnittavstemmingService.utførGrensesnittavstemming(request) }
            .fold(
                onFailure = { illegalState("Grensesnittavstemming feilet", it) },
                onSuccess = { ok("Grensesnittavstemming sendt ok") }
            )
    }

    companion object {

        val LOG: Logger = LoggerFactory.getLogger(AvstemmingController::class.java)
    }
}
