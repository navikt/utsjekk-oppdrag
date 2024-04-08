package no.nav.utsjekk.simulering.simulering

import com.ctc.wstx.exc.WstxEOFException
import com.ctc.wstx.exc.WstxIOException
import jakarta.xml.ws.WebServiceException
import jakarta.xml.ws.soap.SOAPFaultException
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningFeilUnderBehandling
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerFpService
import no.nav.utsjekk.simulering.simulering.dto.SimuleringRequestBody
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.net.SocketException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLException

private val secureLogger = LoggerFactory.getLogger("secureLogger")

@Service
class SimuleringService(private val simulerFpService: SimulerFpService) {
    fun simuler(request: SimuleringRequestBody) =
        try {
            SimuleringRequestBuilder(request).build().let {
                simulerFpService.simulerBeregning(it).tilSimulering()
            }
        } catch (e: SimulerBeregningFeilUnderBehandling) {
            with(e.faultInfo.errorMessage) {
                when {
                    contains("Personen finnes ikke") -> throw PersonFinnesIkkeException(this)
                    contains("ugyldig") -> throw RequestErUgyldigException(this)
                    else -> throw e
                }
            }
        } catch (e: SOAPFaultException) {
            logSoapFaultException(e)
            if (e.cause is WstxEOFException || e.cause is WstxIOException) {
                throw OppdragErStengtException()
            }
            throw e
        } catch (e: WebServiceException) {
            if (e.cause is SSLException || e.cause is SocketException || e.cause is SocketTimeoutException) {
                throw OppdragErStengtException()
            }
            throw e
        }
}

private class PersonFinnesIkkeException(feilmelding: String) : ResponseStatusException(HttpStatus.NOT_FOUND, feilmelding)

private class RequestErUgyldigException(feilmelding: String) : ResponseStatusException(HttpStatus.BAD_REQUEST, feilmelding)

private class OppdragErStengtException() : ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Oppdrag/UR er stengt")

private fun logSoapFaultException(e: SOAPFaultException) {
    val details =
        e.fault.detail?.detailEntries?.asSequence()?.mapNotNull { it.textContent }?.joinToString(",")
    secureLogger.error(
        "SOAPFaultException -" +
            " faultCode=${e.fault.faultCode}" +
            " faultString=${e.fault.faultString}" +
            " details=$details",
    )
}
