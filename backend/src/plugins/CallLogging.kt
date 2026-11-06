package nl.brul.backend.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import org.slf4j.event.Level

fun Application.configureCallLogging() {
    install(CallLogging) {
        level = Level.INFO
        // Plain text instead of ANSI colors: Ktor would otherwise load the native Jansi console library,
        // which makes newer JVMs print "restricted method" warnings at startup.
        disableDefaultColors()
        // Only method, path and status: never headers (Authorization), query strings or bodies.
        format { call ->
            "${call.request.httpMethod.value} ${call.request.path()} -> ${call.response.status()}"
        }
    }
}
