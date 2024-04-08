package no.nav.utsjekk.felles.api

import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ResponseStatusException

@ControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(JwtTokenUnauthorizedException::class)
    fun handleJwtTokenUnauthorizedException(exception: JwtTokenUnauthorizedException): ResponseEntity<String> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized")

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(exception: ResponseStatusException): ResponseEntity<String> =
        ResponseEntity.status(exception.statusCode).body(exception.reason)
}
