package no.nav.dagpenger.oppdrag.repository

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

@Profile("jdbc-test")
@Configuration
class OppdragLagerTestConfig {

    @Bean
    fun oppdragLager(jdbcTemplate: JdbcTemplate, namedParameterJdbcTemplate: NamedParameterJdbcTemplate): OppdragLagerRepository {
        return OppdragLagerRepositoryJdbc(jdbcTemplate, namedParameterJdbcTemplate)
    }
}
