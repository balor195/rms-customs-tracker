package com.rms.customs.di

import com.rms.customs.data.network.ServerUrlHolder
import com.rms.customs.data.network.ServerUrlPlugin
import com.rms.customs.data.remote.api.CustomsApi
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

private val json = Json {
    ignoreUnknownKeys = true
    isLenient         = true
    encodeDefaults    = true
}

val networkModule = module {

    single { ServerUrlHolder(androidContext()) }

    single {
        val urlHolder = get<ServerUrlHolder>()
        HttpClient(OkHttp) {
            install(HttpTimeout) {
                connectTimeoutMillis = 30_000
                socketTimeoutMillis  = 30_000
                requestTimeoutMillis = 30_000
            }
            install(ContentNegotiation) { json(json) }
            install(Logging) { level = LogLevel.BODY }
            install(ServerUrlPlugin) { holder = urlHolder }
        }
    }

    single { CustomsApi(get()) }
}
