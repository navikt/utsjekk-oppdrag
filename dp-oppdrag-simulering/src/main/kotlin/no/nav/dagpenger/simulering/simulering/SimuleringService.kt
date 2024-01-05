package no.nav.dagpenger.simulering.simulering

import jakarta.xml.ws.soap.SOAPFaultException
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerFpService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SimuleringService(private val simulerFpService: SimulerFpService) {
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun simuler(request: SimuleringRequestBody) =
        try {
            SimuleringRequestBuilder(request).build().let {
                simulerFpService.simulerBeregning(it).tilSimulering()
            }
        } catch (e: SOAPFaultException) {
            secureLogger.error("", e)
            throw e
        }
}
