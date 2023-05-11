package no.nav.dagpenger.oppdrag.rest

import no.nav.dagpenger.oppdrag.common.Ressurs
import no.nav.dagpenger.oppdrag.common.RessursUtils.illegalState
import no.nav.dagpenger.oppdrag.common.RessursUtils.ok
import no.nav.dagpenger.oppdrag.domene.GrensesnittavstemmingRequest
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

    /**
     * Konsistensavstemmingen virker i to moduser; en hvor avstemmingen sendes i en batch og en hvor batchen er splittet opp i flere batcher.
     * Første modusen gjør et kall til denne funksjonen og blir trigger hvis både sendStartmelding og sendAvsluttmelding er satt til true.
     * Andre modusen gjør flere kalle (en per delbranch) til denne funksjonen hvor sendStartmelding og sendAvsluttmelding skal settes som følger:
     * Første kallet: sendStartmelding=true og sendAvsluttmelding = false
     * Siste kallet: sendStartmelding=true og sendAvsluttmelding = false
     * Resterende kall: sendStartmelding=false og sendAvsluttmelding = false
     *
     * transaksjonsId må være satt hvis det er en splittet batch.
     */

    companion object {

        val LOG: Logger = LoggerFactory.getLogger(AvstemmingController::class.java)
    }
}
