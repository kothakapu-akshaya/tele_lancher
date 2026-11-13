package com.telelauncher.adblock

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

data class SourceResult(val url: String, val domains: Set<String>, val error: String? = null)

class SourceUpdateManager(private val client: OkHttpClient = OkHttpClient()) {
    suspend fun download(url: String): SourceResult = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext SourceResult(url, emptySet(), "HTTP ${resp.code}")
                val text = resp.body?.string() ?: ""
                SourceResult(url, HostsParser.parse(text))
            }
        } catch (e: IOException) {
            SourceResult(url, emptySet(), e.message)
        }
    }

    fun merge(results: List<SourceResult>, store: BlocklistStore): List<SourceResult> {
        val ok = results.filter { it.error == null }
        if (ok.isEmpty()) return results // preserve previous
        for (r in ok) store.addSource(r.domains)
        return results
    }
}