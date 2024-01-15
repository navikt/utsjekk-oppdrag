package no.nav.dagpenger.oppdrag.iverksetting

import jakarta.validation.Valid
import no.nav.dagpenger.kontrakter.oppdrag.OppdragId
import no.nav.dagpenger.kontrakter.oppdrag.Utbetalingsoppdrag
import no.nav.dagpenger.oppdrag.iverksetting.domene.OppdragMapper
import no.nav.dagpenger.oppdrag.iverksetting.domene.fagsystemId
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@ProtectedWithClaims(issuer = "azuread")
internal class OppdragController(
    @Autowired val oppdragService: OppdragService,
) {
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], path = ["/oppdrag"])
    fun sendOppdrag(
        @Valid @RequestBody utbetalingsoppdrag: Utbetalingsoppdrag,
    ) = Result.runCatching { opprettOppdrag(utbetalingsoppdrag, 0) }.fold(
        onFailure = {
            if (it is OppdragAlleredeSendtException) {
                OppdragAlleredeSendtResponse(utbetalingsoppdrag.fagsystemId)
            } else {
                KlarteIkkeSendeOppdragResponse(utbetalingsoppdrag.fagsystemId)
            }
        },
        onSuccess = {
            ResponseEntity.ok("Oppdrag sendt OK")
        },
    )

    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], path = ["/oppdragPaaNytt/{versjon}"])
    fun sendOppdragPåNytt(
        @Valid @RequestBody utbetalingsoppdrag: Utbetalingsoppdrag,
        @PathVariable versjon: Int,
    ) = Result.runCatching { opprettOppdrag(utbetalingsoppdrag, versjon) }.fold(
        onFailure = {
            KlarteIkkeSendeOppdragResponse(utbetalingsoppdrag.fagsystemId)
        },
        onSuccess = {
            ResponseEntity.ok("Oppdrag sendt OK")
        },
    )

    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], path = ["/status"])
    fun hentStatus(
        @Valid @RequestBody oppdragId: OppdragId,
    ) = Result.runCatching { oppdragService.hentStatusForOppdrag(oppdragId) }
        .fold(
            onFailure = {
                ResponseEntity.status(HttpStatus.NOT_FOUND).body("Fant ikke oppdrag med id $oppdragId")
            },
            onSuccess = {
                ResponseEntity.ok("${it.status}, ${it.kvitteringsmelding?.beskrMelding ?: " Savner kvitteringsmelding"}")
            },
        )

    private fun opprettOppdrag(
        utbetalingsoppdrag: Utbetalingsoppdrag,
        versjon: Int,
    ) {
        val oppdrag110 = OppdragMapper.tilOppdrag110(utbetalingsoppdrag)
        val oppdrag = OppdragMapper.tilOppdrag(oppdrag110)

        oppdragService.opprettOppdrag(utbetalingsoppdrag, oppdrag, versjon)
    }
}

private class OppdragAlleredeSendtResponse(fagsystemId: String) : ResponseEntity<String>(
    "Oppdrag er allerede sendt for saksnr $fagsystemId.",
    HttpStatus.CONFLICT,
)

private class KlarteIkkeSendeOppdragResponse(fagsystemId: String) : ResponseEntity<String>(
    "Klarte ikke sende oppdrag for saksnr $fagsystemId.",
    HttpStatus.INTERNAL_SERVER_ERROR,
)
