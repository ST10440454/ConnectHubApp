package com.connecthub.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Translates English text into another language. */
interface TextTranslator {
    /** Returns translations in the same order as [texts]. Throws [IOException] on failure. */
    suspend fun translate(texts: List<String>, targetLang: String): List<String>
}

/**
 * DeepL API client (Free plan keys end in ":fx" and use api-free.deepl.com).
 *
 * AF, XH and ZU are DeepL "extended" languages: they require the quality_optimized model.
 */
class DeepLTranslationService(private val apiKey: String) : TextTranslator {

    private val host: String
        get() = if (apiKey.endsWith(":fx")) "https://api-free.deepl.com" else "https://api.deepl.com"

    override suspend fun translate(texts: List<String>, targetLang: String): List<String> =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) throw IOException("DEEPL_API_KEY is not set in local.properties")
            texts.chunked(MAX_TEXTS_PER_REQUEST).flatMap { translateBatch(it, targetLang) }
        }

    private fun translateBatch(batch: List<String>, targetLang: String): List<String> {
        val body = JSONObject()
            .put("text", JSONArray(batch))
            .put("source_lang", "EN")
            .put("target_lang", targetLang)
            .put("model_type", "quality_optimized")

        val connection = (URL("$host/v2/translate").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("Authorization", "DeepL-Auth-Key $apiKey")
            setRequestProperty("Content-Type", "application/json")
        }
        try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("DeepL request failed (HTTP $code)")
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val translations = JSONObject(response).getJSONArray("translations")
            if (translations.length() != batch.size) throw IOException("DeepL returned an unexpected result")
            return List(translations.length()) { translations.getJSONObject(it).getString("text") }
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val MAX_TEXTS_PER_REQUEST = 50
    }
}
