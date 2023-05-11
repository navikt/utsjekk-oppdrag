package no.nav.dagpenger.oppdrag.config

import no.nav.common.cxf.StsConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ServiceConfig(
    @Value("\${SECURITYTOKENSERVICE_URL}") private val stsUrl: String,
    @Value("\${SERVICEUSER_USERNAME}") private val systemuserUsername: String,
    @Value("\${SERVICEUSER_PASSWORD}") private val systemuserPwd: String,
    @Value("\${OPPDRAG_SERVICE_URL}") private val simulerFpServiceUrl: String
) {

    init {
        System.setProperty("no.nav.modig.security.sts.url", stsUrl)
        System.setProperty("no.nav.modig.security.systemuser.username", systemuserUsername)
        System.setProperty("no.nav.modig.security.systemuser.password", systemuserPwd)
    }

    @Bean
    fun stsConfig(): StsConfig {
        return StsConfig.builder()
            .url(stsUrl)
            .username(systemuserUsername)
            .password(systemuserPwd)
            .build()
    }
}
