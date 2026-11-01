package com.connecthub.app.domain.model

/**
 * A language the user can select in Settings.
 *
 * [deeplCode] is the DeepL target-language code used to translate the app's English UI
 * text at runtime; it is null for English, which needs no translation.
 */
data class Language(val code: String, val displayName: String, val deeplCode: String? = null) {
    val isEnglish: Boolean get() = deeplCode == null
}

/**
 * English plus the South African official languages that the DeepL API supports.
 *
 * DeepL supports only Afrikaans, isiXhosa and isiZulu out of South Africa's 11 official
 * languages, so those are the only ones offered. The UI text is translated at runtime by
 * [com.connecthub.app.data.i18n.LanguageManager] and cached on the device; there are no
 * per-language values-<code> resource folders any more.
 *
 * Translations are machine-generated: have a native speaker review them before relying
 * on them for a real audience.
 */
object SupportedLanguages {
    val ALL = listOf(
        Language("en", "English"),
        Language("af", "Afrikaans", "AF"),
        Language("xh", "isiXhosa", "XH"),
        Language("zu", "isiZulu", "ZU")
    )

    fun byCode(code: String): Language = ALL.firstOrNull { it.code == code } ?: ALL.first()
}
