package no.nav.dagpenger.oppdrag.iverksetting.tilstand

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate

@Configuration
internal class OppdragLagerTestConfig {
    @Bean
    fun oppdragLager(jdbcTemplate: JdbcTemplate) = OppdragLagerRepository(jdbcTemplate)
}
