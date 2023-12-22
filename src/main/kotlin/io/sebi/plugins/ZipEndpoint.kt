package io.sebi.plugins

import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

@Resource("/repo")
data class RepoPath(val user: String, val name: String, val branch: String, val path: String)

fun Application.configureZipEndpoint() {
    routing {
        route("zip") {
            get<RepoPath> { repoPath ->
                // TODO: Return actual zipped file!
                call.respondText(listAllFiles(repoPath).toString())
            }
        }
    }
}