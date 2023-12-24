package io.sebi.di

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import io.sebi.common.Constant
import io.sebi.data.GitHubFolderDownloaderRepositoryImpl
import io.sebi.domain.repository.GitHubFolderDownloaderRepository
import org.koin.dsl.module
import java.io.File
import java.util.concurrent.TimeUnit


val koinModule = module {

    single {
        HttpClient(OkHttp) {

            // To accommodate machine with slower internet connections, increasing the timeout duration
            // can help prevent potential exceptions due to extended response times.
            engine {
                config {
                    connectTimeout(Constant.CUSTOM_TIMEOUT, TimeUnit.MILLISECONDS)
                    readTimeout(Constant.CUSTOM_TIMEOUT, TimeUnit.MILLISECONDS)
                    writeTimeout(Constant.CUSTOM_TIMEOUT, TimeUnit.MILLISECONDS)
                }
            }

            install (ContentNegotiation) {
                json()
            }
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.INFO
            }
            defaultRequest {
                val token = File("key.local").readLines().first().trim()

                headers {
                    set("Authorization", "Bearer $token")
                }
            }

        }
    }

    single<GitHubFolderDownloaderRepository> {
        GitHubFolderDownloaderRepositoryImpl(get())
    }

}