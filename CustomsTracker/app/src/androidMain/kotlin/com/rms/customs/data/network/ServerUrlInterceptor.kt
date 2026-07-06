package com.rms.customs.data.network

import android.content.Context
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response

class ServerUrlInterceptor(
    private val context: Context,
) : Interceptor {

    companion object {
        const val PREFS_NAME  = "rms_app_config"
        const val KEY_URL     = "server_url"
        const val DEFAULT_URL = "http://10.0.2.2:8000/"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val raw = readUrl()
        return try {
            val parsed = raw.toHttpUrl()
            val orig   = chain.request()
            val newUrl = orig.url.newBuilder()
                .scheme(parsed.scheme)
                .host(parsed.host)
                .port(parsed.port)
                .build()
            chain.proceed(orig.newBuilder().url(newUrl).build())
        } catch (e: Exception) {
            chain.proceed(chain.request())
        }
    }

    fun readUrl(): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_URL, DEFAULT_URL) ?: DEFAULT_URL

    fun saveUrl(url: String) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_URL, url.trimEnd('/') + "/").apply()
}
