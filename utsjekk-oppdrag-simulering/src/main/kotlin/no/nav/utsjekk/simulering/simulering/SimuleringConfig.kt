package no.nav.utsjekk.simulering.simulering

import no.nav.common.cxf.CXFClient
import no.nav.common.cxf.StsConfig
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerFpService
import org.apache.cxf.interceptor.LoggingOutInterceptor
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class SimuleringConfig(
    @Value("\${SECURITYTOKENSERVICE_URL}") private val stsUrl: String,
    @Value("\${srv.username}") private val systemuserUsername: String,
    @Value("\${srv.password}") private val systemuserPwd: String,
    @Value("\${OPPDRAG_SERVICE_URL}") private val simulerFpServiceUrl: String,
) {
    @Bean
    @Profile("!local")
    fun simulerFpService(): SimulerFpService =
        CXFClient(SimulerFpService::class.java)
            .address(simulerFpServiceUrl)
            .timeout(20000, 20000)
            .configureStsForSystemUser(stsConfig())
            .withOutInterceptor(LoggingOutInterceptor())
            .build()

    private fun stsConfig(): StsConfig =
        StsConfig.builder()
            .url(stsUrl)
            .username(systemuserUsername)
            .password(systemuserPwd)
            .build()
}
