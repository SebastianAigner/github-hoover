package io.sebi.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.sebi.domain.downloader.GitHubFolderDownloader
import io.sebi.routes.downloadZipEndpoint
import kotlinx.html.body
import kotlinx.html.h1
import org.koin.ktor.ext.inject

fun Application.configureRouting() {

    val repository: GitHubFolderDownloader by inject()

    routing {
        get("/healthz") {
            call.respondText("OK")
        }
        downloadZipEndpoint(repository)
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            cause.printStackTrace()
            call.respondHtml(HttpStatusCode.InternalServerError) {
                body {
                    h1 { +"500 Internal Server Error" }
//                    cause.stackTraceToString().lines().forEach {
//                        pre { +it }
//                    }
                }
            }
        }
    }

}