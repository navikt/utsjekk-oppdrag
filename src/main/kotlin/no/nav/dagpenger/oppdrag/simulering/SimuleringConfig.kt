package no.nav.dagpenger.oppdrag.simulering

import no.nav.common.cxf.CXFClient
import no.nav.common.cxf.StsConfig
import no.nav.system.os.eksponering.simulerfpservicewsbinding.SimulerFpService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class SimuleringConfig(
    @Value("\${SECURITYTOKENSERVICE_URL}") private val stsUrl: String,
    @Value("\${SERVICEUSER_USERNAME}") private val systemuserUsername: String,
    @Value("\${SERVICEUSER_PASSWORD}") private val systemuserPwd: String,
    @Value("\${OPPDRAG_SERVICE_URL}") private val simulerFpServiceUrl: String,
) {

    @Bean
    @Profile("!local")
    fun simulerFpService(): SimulerFpService =
        CXFClient(SimulerFpService::class.java)
            .address(simulerFpServiceUrl)
            .timeout(20000, 20000)
            .configureStsForSystemUser(stsConfig())
            .build()

    private fun stsConfig(): StsConfig =
        StsConfig.builder()
            .url(stsUrl)
            .username(systemuserUsername)
            .password(systemuserPwd)
            .build()
}
