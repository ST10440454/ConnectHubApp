package com.connecthub.app.data.i18n

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import com.connecthub.app.data.remote.TextTranslator
import com.connecthub.app.domain.model.Language
import com.connecthub.app.domain.model.SupportedLanguages
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import java.io.File
import java.util.Locale

/**
 * Runtime UI translation.
 *
 * The app ships English string resources only. When the user picks another language the
 * English strings are translated once through DeepL, cached in the app's private storage
 * (so later launches and offline use need no network), and then applied to every screen by
 * [translateViewTree] and [tr].
 *
 * Translations are keyed by the English text itself, so a view that still shows its
 * original English text can be looked up directly.
 */
class LanguageManager(context: Context, private val translator: TextTranslator) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("connecthub_language", Context.MODE_PRIVATE)

    var current: Language = SupportedLanguages.byCode(prefs.getString(KEY_CODE, "en").orEmpty())
        private set

    private var translations: Map<String, String> =
        if (current.isEnglish) emptyMap() else loadCache(current.code)

    /** Looks up a string resource in the current language, formatting [args] if given. */
    fun tr(@StringRes id: Int, vararg args: Any): String {
        val english = appContext.getString(id)
        val text = translations[english] ?: english
        return if (args.isEmpty()) text else String.format(Locale.getDefault(), text, *args)
    }

    /**
     * Switches language. Translates (and caches) anything not already cached.
     * Returns false, leaving the current language unchanged, if translation fails.
     */
    suspend fun setLanguage(language: Language): Boolean {
        val targetCode = language.deeplCode
        if (targetCode == null) {
            commit(language, emptyMap())
            return true
        }
        val cached = loadCache(language.code)
        val missing = TranslatableStrings.englishTexts(appContext).filter { it !in cached }
        var merged = cached
        if (missing.isNotEmpty()) {
            val translated = try {
                translator.translate(missing, targetCode)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                return false
            }
            // Never trust a result that dropped a format argument such as %1$s: keep English.
            val safe = missing.zip(translated).associate { (source, result) ->
                source to if (formatArgs(result) == formatArgs(source)) result else source
            }
            merged = cached + safe
            saveCache(language.code, merged)
        }
        commit(language, merged)
        return true
    }

    /** Translates every English text still visible in [view] and its descendants. */
    fun translateViewTree(view: View) {
        if (translations.isEmpty()) return
        translateNode(view)
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) translateViewTree(view.getChildAt(i))
        }
    }

    private fun translateNode(view: View) {
        view.contentDescription?.toString()?.let { t -> translations[t]?.let { view.contentDescription = it } }
        when (view) {
            is TextInputLayout -> {
                view.hint?.toString()?.let { t -> translations[t]?.let { view.hint = it } }
                view.helperText?.toString()?.let { t -> translations[t]?.let { view.helperText = it } }
            }
            is EditText -> view.hint?.toString()?.let { t -> translations[t]?.let { view.hint = it } }
            is TextView -> {
                view.text?.toString()?.let { t -> translations[t]?.let { view.text = it } }
                view.hint?.toString()?.let { t -> translations[t]?.let { view.hint = it } }
            }
            is Toolbar -> {
                view.title?.toString()?.let { t -> translations[t]?.let { view.title = it } }
                val menu = view.menu
                for (i in 0 until menu.size()) {
                    val item = menu.getItem(i)
                    item.title?.toString()?.let { t -> translations[t]?.let { item.title = it } }
                }
            }
        }
    }

    private fun commit(language: Language, map: Map<String, String>) {
        prefs.edit().putString(KEY_CODE, language.code).apply()
        current = language
        translations = map
    }

    private fun cacheFile(code: String) = File(appContext.filesDir, "translations_$code.json")

    private fun loadCache(code: String): Map<String, String> = try {
        val file = cacheFile(code)
        if (!file.exists()) emptyMap() else {
            val json = JSONObject(file.readText())
            json.keys().asSequence().associateWith { json.getString(it) }
        }
    } catch (e: Exception) {
        emptyMap()
    }

    private fun saveCache(code: String, map: Map<String, String>) {
        try {
            cacheFile(code).writeText(JSONObject(map).toString())
        } catch (e: Exception) {
            // Cache is an optimisation only; the translations still apply this session.
        }
    }

    private fun formatArgs(s: String): List<String> =
        Regex("%\\d+\\$[sdf]|%[sdf]").findAll(s).map { it.value }.sorted().toList()

    private companion object {
        const val KEY_CODE = "code"
    }
}
