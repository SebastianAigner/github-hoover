package io.sebi.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.sebi.domain.repository.GitHubFolderDownloaderRepository
import io.sebi.routes.downloadZipEndpoint
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.pre
import org.koin.ktor.ext.inject

fun Application.configureRouting() {

    val repository: GitHubFolderDownloaderRepository by inject()

    routing {

        downloadZipEndpoint(repository)

    }

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