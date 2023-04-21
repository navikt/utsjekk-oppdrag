package dp.oppdrag.sender

import dp.oppdrag.utils.getProperty
import no.nav.system.os.eksponering.simulerfpservicewsbinding.SimulerFpService
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningRequest
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningResponse
import no.nav.common.cxf.CXFClient
import no.nav.common.cxf.StsConfig
import javax.xml.namespace.QName

class SimuleringSenderImpl : SimuleringSender {

    private lateinit var port: SimulerFpService

    override fun hentSimulerBeregningResponse(simulerBeregningRequest: SimulerBeregningRequest?): SimulerBeregningResponse {
        if (!::port.isInitialized) {
            port =
                CXFClient(SimulerFpService::class.java)
                    .wsdl("classpath:wsdl/no/nav/system/os/eksponering/simulerfpservicewsbinding.wsdl")
                    .serviceName(QName("http://nav.no/system/os/eksponering/simulerFpServiceWSBinding", "simulerFpService"))
                    .endpointName(QName("http://nav.no/system/os/eksponering/simulerFpServiceWSBinding", "simulerFpServicePort"))
                    .address(getProperty("OPPDRAG_SERVICE_URL"))
                    .timeout(20000, 20000)
                    .configureStsForSystemUser(
                        StsConfig.builder()
                            .url(getProperty("STS_URL"))
                            .username(getProperty("MQ_USER"))
                            .password(getProperty("MQ_PASSWORD"))
                            .build()
                    )
                    //.withOutInterceptor(LoggingOutInterceptor())
                    .build()
        }

        return port.simulerBeregning(simulerBeregningRequest)
    }
}
