package dp.oppdrag.sender

import dp.oppdrag.defaultLogger
import dp.oppdrag.utils.getProperty
import no.nav.common.cxf.CXFClient
import no.nav.common.cxf.StsConfig
import no.nav.system.os.eksponering.simulerfpservicewsbinding.SimulerFpService
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningRequest
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningResponse
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.EMPTY_REQUEST
import java.io.IOException
import java.util.*

class SimuleringSenderImpl : SimuleringSender {

    private lateinit var port: SimulerFpService

    override fun hentSimulerBeregningResponse(simulerBeregningRequest: SimulerBeregningRequest?): SimulerBeregningResponse {
        if (!::port.isInitialized) {
            defaultLogger.info { "########## " + getProperty("STS_URL") }

            val base = "${getProperty("MQ_USER")}:${getProperty("MQ_PASSWORD")}"
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(getProperty("STS_URL")!!)
                .header("Accept", "application/json; charset=UTF-8")
                .header("Authorization", "Basic ${Base64.getEncoder().encodeToString(base.toByteArray())}")
                .post(EMPTY_REQUEST)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")

                defaultLogger.info { "¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤ " + response.body!!.string()}
            }

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
