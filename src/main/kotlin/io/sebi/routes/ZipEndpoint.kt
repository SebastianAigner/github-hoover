package io.sebi.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.sebi.domain.downloader.GitHubFolderDownloader
import io.sebi.domain.model.RepoPath
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths


fun Route.downloadZipEndpoint(repository: GitHubFolderDownloader) {

    route("/download-zip"){
        get<RepoPath> {
            val outputPath = getTempDirectoryPath().plus(File.pathSeparator).plus(getLastPartOfPath(it.path))
            repository.downloadFilesAsZip(outputPath, it.user, it.name, it.branch, it.path)

            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Attachment
                    .withParameter(ContentDisposition.Parameters.FileName, getLastPartOfPath(it.path))
                    .toString()
            )
            call.respondFile(File(outputPath))

        }
    }
}

fun getTempDirectoryPath(): String {
    return Files.createTempDirectory("zip").toAbsolutePath().toString()
}

fun getLastPartOfPath(path: String): String {
    val normalizedPath = Paths.get(path).normalize()
    return normalizedPath.fileName.toString().plus(".zip")
}