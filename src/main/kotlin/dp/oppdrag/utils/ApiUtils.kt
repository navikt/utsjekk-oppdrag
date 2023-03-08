package dp.oppdrag.utils

import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext
import dp.oppdrag.defaultLogger
import io.ktor.http.*


fun isCurrentlyRunningOnNais(): Boolean {
    return System.getenv("NAIS_APP_NAME") != null
}

suspend inline fun <reified TResponse : Any> OpenAPIPipelineResponseContext<TResponse>.respondError(
    message: String,
    throwable: Throwable? = null
) {
    defaultLogger.error { throwable }

    responder.respond(
        HttpStatusCode.InternalServerError,
        message,
        this.pipeline
    )
}

suspend inline fun <reified TResponse : Any> OpenAPIPipelineResponseContext<TResponse>.respondConflict(message: String) {
    responder.respond(
        HttpStatusCode.Conflict,
        message,
        this.pipeline
    )
}