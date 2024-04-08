package no.nav.utsjekk.simulering.config

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import no.nav.utsjekk.felles.log.filter.LogFilter
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.boot.web.servlet.server.ServletWebServerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan

@SpringBootConfiguration
@ComponentScan("no.nav.utsjekk.simulering", "no.nav.utsjekk.felles")
@EnableJwtTokenValidation(ignore = ["org.springframework", "org.springdoc"])
class ApplicationConfig {
    @Bean
    fun servletWebServerFactory(): ServletWebServerFactory = JettyServletWebServerFactory()

    @Bean
    fun logFilter() =
        FilterRegistrationBean<LogFilter>().apply {
            filter = LogFilter()
            order = 1
        }
}
