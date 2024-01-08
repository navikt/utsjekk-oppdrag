package no.nav.dagpenger.simulering.simulering

import com.ctc.wstx.exc.WstxEOFException
import com.ctc.wstx.exc.WstxIOException
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.ServiceUnavailableException
import jakarta.xml.ws.soap.SOAPFaultException
import no.nav.dagpenger.simulering.simulering.dto.SimuleringRequestBody
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningFeilUnderBehandling
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
        } catch (e: SimulerBeregningFeilUnderBehandling) {
            with(e.faultInfo.errorMessage) {
                when {
                    contains("Personen finnes ikke") -> throw NotFoundException(this)
                    contains("ugyldig") -> throw BadRequestException(this)
                    else -> throw e
                }
            }
        } catch (e: SOAPFaultException) {
            secureLogger.error("", e)
            if (e.cause is WstxEOFException || e.cause is WstxIOException) {
                throw ServiceUnavailableException("Oppdrag/UR er stengt")
            }
            throw e
        }
}
