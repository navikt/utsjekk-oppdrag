package no.nav.dagpenger.oppdrag.iverksetting

import no.nav.dagpenger.oppdrag.iverksetting.tilstand.OppdragLagerRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate

@Profile("jdbc-test")
@Configuration
internal class OppdragLagerTestConfig {
    @Bean
    fun oppdragLager(jdbcTemplate: JdbcTemplate) = OppdragLagerRepository(jdbcTemplate)
}
