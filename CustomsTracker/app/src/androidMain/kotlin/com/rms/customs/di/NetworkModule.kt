package com.rms.customs.di

import com.rms.customs.data.network.ServerUrlInterceptor
import com.rms.customs.data.remote.api.CustomsApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

// Placeholder base URL — ServerUrlInterceptor rewrites host/port per-request from SharedPreferences.
private const val PLACEHOLDER_URL = "http://rms.internal/"

private val json = Json {
    ignoreUnknownKeys = true
    isLenient         = true
    encodeDefaults    = true
}

val networkModule = module {

    single { ServerUrlInterceptor(androidContext()) }

    single {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(get<ServerUrlInterceptor>())
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    single {
        Retrofit.Builder()
            .baseUrl(PLACEHOLDER_URL)
            .client(get<OkHttpClient>())
            .addConverterFactory(json.asConverterFactory("application/json; charset=UTF8".toMediaType()))
            .build()
    }

    single { get<Retrofit>().create(CustomsApi::class.java) }
}
