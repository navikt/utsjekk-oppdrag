package no.nav.dagpenger.oppdrag.simulering

import jakarta.xml.ws.soap.SOAPFaultException
import no.nav.system.os.eksponering.simulerfpservicewsbinding.SimulerFpService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SimuleringService(private val simulerFpService: SimulerFpService) {
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun simuler(request: SimuleringRequestBody): Simulering? {
        return try {
            SimuleringRequestBuilder(request).build().let {
                simulerFpService.simulerBeregning(it).tilSimulering()
            }
        } catch (e: SOAPFaultException) {
            secureLogger.error("", e)
            throw e
        }
    }
}
