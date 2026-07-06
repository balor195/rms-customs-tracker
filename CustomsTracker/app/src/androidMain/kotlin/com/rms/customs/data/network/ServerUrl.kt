package com.rms.customs.data.network

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.Url
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val Context.appConfigDataStore by preferencesDataStore(name = "rms_app_config")

class ServerUrlHolder(private val context: Context) {

    companion object {
        val KEY_URL = stringPreferencesKey("server_url")
        const val DEFAULT_URL = "http://10.0.2.2:8000/"
    }

    // Cached in memory so readUrl()/saveUrl() can stay synchronous (matching the old
    // SharedPreferences-backed API) even though DataStore itself is Flow/suspend-based.
    @Volatile
    private var cachedUrl: String = runBlocking {
        context.appConfigDataStore.data.first()[KEY_URL] ?: DEFAULT_URL
    }

    fun readUrl(): String = cachedUrl

    fun saveUrl(url: String) {
        val normalized = url.trimEnd('/') + "/"
        cachedUrl = normalized
        CoroutineScope(Dispatchers.IO).launch {
            context.appConfigDataStore.edit { prefs -> prefs[KEY_URL] = normalized }
        }
    }
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
