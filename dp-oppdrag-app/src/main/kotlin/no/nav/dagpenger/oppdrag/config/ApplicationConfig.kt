package no.nav.dagpenger.oppdrag.config

import no.nav.dagpenger.oppdrag.common.log.filter.LogFilter
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.boot.web.servlet.server.ServletWebServerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootConfiguration
@EntityScan(ApplicationConfig.PAKKENAVN, "no.nav.dagpenger.oppdrag.sikkerhet")
@ComponentScan(ApplicationConfig.PAKKENAVN)
@EnableScheduling
@EnableJwtTokenValidation(ignore = ["org.springframework", "org.springdoc"])
class ApplicationConfig {

    @Bean
    fun servletWebServerFactory(): ServletWebServerFactory = JettyServletWebServerFactory()

    @Bean
    fun logFilter(): FilterRegistrationBean<LogFilter> {
        val filterRegistration = FilterRegistrationBean<LogFilter>()
        filterRegistration.filter = LogFilter()
        filterRegistration.order = 1
        return filterRegistration
    }

    companion object {
        const val PAKKENAVN = "no.nav.dagpenger.oppdrag"
        val LOKALE_PROFILER = setOf("local", "e2e")
    }
}
