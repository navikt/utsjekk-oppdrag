package no.nav.utsjekk.oppdrag.config

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import no.nav.utsjekk.felles.log.filter.LogFilter
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.boot.web.servlet.server.ServletWebServerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootConfiguration
@EntityScan("no.nav.utsjekk.oppdrag")
@ComponentScan("no.nav.utsjekk.oppdrag", "no.nav.utsjekk.felles")
@EnableScheduling
@EnableJwtTokenValidation(ignore = ["org.springframework", "org.springdoc"])
internal class ApplicationConfig {
    @Bean
    fun servletWebServerFactory(): ServletWebServerFactory = JettyServletWebServerFactory()

    @Bean
    fun logFilter() =
        FilterRegistrationBean<LogFilter>().apply {
            filter = LogFilter()
            order = 1
        }
}
