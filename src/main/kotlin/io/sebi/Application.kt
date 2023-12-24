package io.sebi

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.resources.*
import io.sebi.plugins.configureKoin
import io.sebi.plugins.configureMonitoring
import io.sebi.plugins.configureRouting
import io.sebi.plugins.configureSerialization

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureKoin()
    configureSerialization()
    install(Resources)
    configureMonitoring()
    configureRouting()
}