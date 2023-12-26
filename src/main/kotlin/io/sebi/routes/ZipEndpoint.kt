package io.sebi.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.sebi.domain.downloader.GitHubFolderDownloader
import io.sebi.domain.model.RepoPath
import kotlinx.serialization.json.Json

// Check via env variables
val allowList = listOf(
    RepoPath("SebastianAigner", "", "", ""),
    RepoPath("JetBrains", "amper", "0.1", "examples"),
    RepoPath("JetBrains", "compose-multiplatform", "", "examples/imageviewer"),
    RepoPath("Kotlin", "kotlin-wasm-examples", "", "")
)

private fun RepoPath.isAllowed(): Boolean {
    val allowListText = requireNotNull(System.getenv("HOOVER_ALLOWLIST")) {
        "[HOOVER_ALLOWLIST] env variable is required"
    }
    val allowList = Json.decodeFromString<List<RepoPath>>(allowListText)

    return allowList.any {
        val userAllowed = it.user == this.user
        val repoNameAllowed = if (it.name.isBlank()) true else it.name == this.name
        val branchNameAllowed = if (it.branch.isBlank()) true else it.branch == this.branch
        val pathAllowed =
            if (it.path.isBlank()) true else this.path.removePrefix("/").startsWith(it.path.removePrefix("/"))
        userAllowed && repoNameAllowed && branchNameAllowed && pathAllowed
    }
}

fun Route.downloadZipEndpoint(repository: GitHubFolderDownloader) {

    get("/defaultAllowlist") {
        call.respond(allowList)
    }
    route("/download-zip") {
        get<RepoPath> {
            if (!it.isAllowed()) return@get call.respondText(status = HttpStatusCode.Forbidden) {
                "This instance is configured to only download allowlisted repositories and directories. Feel free to spin up your own instance of this service via https://github.com/SebastianAigner/github-hoover"
            }
            val zipBytes = repository.downloadFilesAsZip(it.user, it.name, it.branch, it.path)

            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Attachment
                    .withParameter(ContentDisposition.Parameters.FileName, it.path.substringAfterLast("/") + ".zip")
                    .toString()
            )
            call.respondBytes(zipBytes)
        }
    }
}