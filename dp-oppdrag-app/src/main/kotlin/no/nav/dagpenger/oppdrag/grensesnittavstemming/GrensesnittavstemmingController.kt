package no.nav.dagpenger.oppdrag.grensesnittavstemming

import no.nav.dagpenger.kontrakter.oppdrag.GrensesnittavstemmingRequest
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@ProtectedWithClaims(issuer = "azuread")
@Validated
internal class GrensesnittavstemmingController(
    @Autowired val service: GrensesnittavstemmingService,
) {
    @PostMapping(path = ["/grensesnittavstemming"])
    fun grensesnittavstemming(
        @RequestBody request: GrensesnittavstemmingRequest,
    ) = Result.runCatching {
        logger.info("Grensesnittavstemming: Kjører for ${request.fagsystem}-oppdrag fra ${request.fra} til ${request.til}")
        service.utførGrensesnittavstemming(fagsystem = request.fagsystem, fra = request.fra, til = request.til)
    }
        .fold(
            onFailure = {
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Grensesnittavstemming feilet")
            },
            onSuccess = { ResponseEntity.ok("Grensesnittavstemming sendt ok") },
        )

    companion object {
        val logger: Logger = LoggerFactory.getLogger(GrensesnittavstemmingController::class.java)
    }
}
