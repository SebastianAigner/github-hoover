package io.sebi

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.resources.*
import io.sebi.plugins.configureKoin
import io.sebi.plugins.configureMonitoring
import io.sebi.plugins.configureRouting
import io.sebi.plugins.configureSerialization
import kotlin.time.Duration.Companion.seconds

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureRateLimit()
    configureKoin()
    configureSerialization()
    install(Resources)
    configureMonitoring()
    configureRouting()
}

private fun Application.configureRateLimit() {
    install(RateLimit) {
        global {
            rateLimiter(30, 1.seconds)
            requestKey { call -> call.request.origin.remoteAddress }
        }
    }
}
