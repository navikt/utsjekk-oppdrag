package no.nav.dagpenger.simulering.config

import no.nav.dagpenger.simulering.common.Ressurs
import no.nav.dagpenger.simulering.common.RessursUtils.illegalState
import no.nav.dagpenger.simulering.common.RessursUtils.unauthorized
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.springframework.core.NestedExceptionUtils.getMostSpecificCause
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(JwtTokenUnauthorizedException::class)
    fun handleJwtTokenUnauthorizedException(jwtTokenUnauthorizedException: JwtTokenUnauthorizedException): ResponseEntity<Ressurs<Nothing>> {
        return unauthorized("Unauthorized")
    }

    @ExceptionHandler(Throwable::class)
    fun handleThrowable(throwable: Throwable): ResponseEntity<Ressurs<Nothing>> {
        val mostSpecificThrowable = getMostSpecificCause(throwable)
        return illegalState(mostSpecificThrowable.message.toString(), mostSpecificThrowable)
    }
}
