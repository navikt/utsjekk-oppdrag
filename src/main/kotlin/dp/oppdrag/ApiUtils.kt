package dp.oppdrag

import com.papsign.ktor.openapigen.modules.registerModule
import com.papsign.ktor.openapigen.route.OpenAPIRoute
import com.papsign.ktor.openapigen.route.modules.PathProviderModule
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import io.ktor.util.*


@KtorDsl
inline fun <TRoute : OpenAPIRoute<TRoute>> TRoute.authenticatedRoute(path: String, crossinline fn: TRoute.() -> Unit) {
    val ch = child(ktorRoute.authenticate { ktorRoute.createRouteFromPath(path) }).apply {
        provider.registerModule(PathProviderModule(path))
    }
    ch.fn()
}

fun isCurrentlyRunningOnNais(): Boolean {
    return System.getenv("NAIS_APP_NAME") != null
}
