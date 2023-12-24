package io.sebi.plugins

import io.ktor.server.application.*
import io.sebi.di.koinModule
import org.koin.ktor.plugin.Koin


fun Application.configureKoin() {
    install(Koin) {
        modules(koinModule)
    }
}