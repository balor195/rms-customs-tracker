package com.rms.customs.data.network

import android.content.Context
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.Url

class ServerUrlHolder(private val context: Context) {

    companion object {
        const val PREFS_NAME  = "rms_app_config"
        const val KEY_URL     = "server_url"
        const val DEFAULT_URL = "http://10.0.2.2:8000/"
    }

    fun readUrl(): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_URL, DEFAULT_URL) ?: DEFAULT_URL

    fun saveUrl(url: String) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_URL, url.trimEnd('/') + "/").apply()
}

class ServerUrlPluginConfig {
    lateinit var holder: ServerUrlHolder
}

val ServerUrlPlugin = createClientPlugin("ServerUrl", ::ServerUrlPluginConfig) {
    val holder = pluginConfig.holder
    onRequest { request, _ ->
        try {
            val parsed = Url(holder.readUrl())
            request.url.protocol = parsed.protocol
            request.url.host = parsed.host
            request.url.port = parsed.port
        } catch (e: Exception) {
            // Keep the request's existing URL if the stored value fails to parse.
        }
    }
}
