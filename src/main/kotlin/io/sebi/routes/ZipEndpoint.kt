package io.sebi.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.sebi.domain.downloader.GitHubFolderDownloader
import io.sebi.domain.model.RepoPath


fun Route.downloadZipEndpoint(repository: GitHubFolderDownloader) {

    route("/download-zip"){
        get<RepoPath> {
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