package no.nav.dagpenger.simulering.config

import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.ServiceUnavailableException
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningFeilUnderBehandling
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(JwtTokenUnauthorizedException::class)
    fun handleJwtTokenUnauthorizedException(exception: JwtTokenUnauthorizedException): ResponseEntity<String> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized")

    @ExceptionHandler(BadRequestException::class)
    fun handleBadRequest(throwable: Throwable): ResponseEntity<String> = ResponseEntity.status(400).body(throwable.message)

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(throwable: Throwable): ResponseEntity<String> = ResponseEntity.status(404).body(throwable.message)

    @ExceptionHandler(ServiceUnavailableException::class)
    fun handleServiceUnavailable(throwable: Throwable): ResponseEntity<String> = ResponseEntity.status(503).body(throwable.message)

    @ExceptionHandler(SimulerBeregningFeilUnderBehandling::class)
    fun handleFeilUnderBehandling(throwable: SimulerBeregningFeilUnderBehandling): ResponseEntity<String> =
        ResponseEntity.status(500).body(throwable.faultInfo.errorMessage)
}
