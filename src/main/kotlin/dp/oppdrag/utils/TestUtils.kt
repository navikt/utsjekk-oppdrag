package dp.oppdrag.utils

import dp.oppdrag.defaultObjectMapper
import dp.oppdrag.model.KvitteringDto
import dp.oppdrag.model.OppdragLagerStatus
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headers
import java.util.concurrent.TimeoutException

inline fun <R> vent(
    antallForsøk: Int = 100,
    sovetidIMillisekunder: Long = 50,
    ferdig: (R) -> Boolean = { it != null },
    block: () -> R
): R {

    (1..antallForsøk).forEach {
        val resultat = block()
        if (ferdig(resultat)) {
            return resultat
        } else {
            Thread.sleep(sovetidIMillisekunder)
        }
    }

    throw TimeoutException("Fikk ikke resultat innen tidsfristen på ${antallForsøk * sovetidIMillisekunder} ms")
}