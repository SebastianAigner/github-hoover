package io.sebi

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.html.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.sebi.plugins.configureZipEndpoint
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.pre
import org.slf4j.event.Level

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }
    install(Resources)
    configureMonitoring()
    configureZipEndpoint()
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondHtml {
                body {
                    h1 { +"500 Internal Server Error" }
                    cause.stackTraceToString().lines().forEach {
                        pre { +it }
                    }
                }
            }
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }
}

fun Application.configureMonitoring() {
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }
}