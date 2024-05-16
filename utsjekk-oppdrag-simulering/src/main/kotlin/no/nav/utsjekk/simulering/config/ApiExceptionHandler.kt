package no.nav.utsjekk.simulering.config

import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningFeilUnderBehandling
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class TestExceptionHandler {
    @ExceptionHandler(SimulerBeregningFeilUnderBehandling::class)
    fun handleFeilUnderBehandling(throwable: SimulerBeregningFeilUnderBehandling): ResponseEntity<String> =
        ResponseEntity.status(500).body(throwable.faultInfo.errorMessage)

    @ExceptionHandler(Throwable::class)
    fun handleThrowable(throwable: Throwable): ResponseEntity<String> =
        ResponseEntity.status(500).body(throwable.message)
}
