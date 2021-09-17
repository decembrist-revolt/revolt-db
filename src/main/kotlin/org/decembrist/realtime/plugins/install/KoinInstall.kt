package org.decembrist.realtime.plugins.install

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.application.*
import org.decembrist.realtime.plugins.configure.Database
import org.decembrist.realtime.plugins.configure.connectionFactory
import org.decembrist.realtime.service.*
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.ktor.ext.Koin
import org.slf4j.Logger

object KoinInstall: Install {

    lateinit var defaultModules: Module.() -> Unit

    override fun Application.install() = install(Koin) {
        defaultModules = {
            single<Logger> { this@install.log }
            single<Database.ConnectionFactory> { this@install.connectionFactory }
            single<ObjectMapper> { this@install.objectMapper }
            single { DataService() }
            single { PathService() }
            single { FetchService(get(), get(), get(), get()) }
            single { InsertService(get(), get(), get(), get()) }
            single { SearchService(get(), get(), get()) }
        }
        modules(module(moduleDeclaration = defaultModules))
    }

}