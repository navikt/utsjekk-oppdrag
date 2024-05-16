package no.nav.utsjekk.simulering.config

import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningFeilUnderBehandling
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class TestExceptionHandler {

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
    @ExceptionHandler(SimulerBeregningFeilUnderBehandling::class)
    fun handleFeilUnderBehandling(throwable: SimulerBeregningFeilUnderBehandling): ResponseEntity<String> {
        logger.warn("Feil i respons fra simulering:", throwable)
        return ResponseEntity.status(500).body(throwable.faultInfo.errorMessage)
    }

    @ExceptionHandler(Throwable::class)
    fun handleThrowable(throwable: Throwable): ResponseEntity<String> {
        logger.warn("Feil i simulering:", throwable)
        return ResponseEntity.status(500).body(throwable.message)
    }
}
