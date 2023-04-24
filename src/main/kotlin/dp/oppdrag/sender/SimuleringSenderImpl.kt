package dp.oppdrag.sender

import dp.oppdrag.defaultLogger
import dp.oppdrag.utils.getProperty
import no.nav.system.os.eksponering.simulerfpservicewsbinding.SimulerFpService
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningRequest
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningResponse
import no.nav.common.cxf.CXFClient
import no.nav.common.cxf.StsConfig

class SimuleringSenderImpl : SimuleringSender {

    private lateinit var port: SimulerFpService

    override fun hentSimulerBeregningResponse(simulerBeregningRequest: SimulerBeregningRequest?): SimulerBeregningResponse {
        if (!::port.isInitialized) {
            defaultLogger.info { "########## " + getProperty("STS_URL") }
            port =
                CXFClient(SimulerFpService::class.java)
                    .address(getProperty("OPPDRAG_SERVICE_URL"))
                    .timeout(20000, 20000)
                    .configureStsForSystemUser(
                        StsConfig.builder()
                            .url(getProperty("STS_URL"))
                            .username(getProperty("MQ_USER"))
                            .password(getProperty("MQ_PASSWORD"))
                            .build()
                    )
                    .build()
        }

        return port.simulerBeregning(simulerBeregningRequest)
    }
}
