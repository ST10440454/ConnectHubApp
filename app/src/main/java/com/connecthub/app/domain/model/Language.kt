package com.connecthub.app.domain.model

/** A language the user can select in Settings, identified by its BCP-47 / ISO 639-1 code. */
data class Language(val code: String, val displayName: String)

/**
 * All 11 official South African languages, matching the multi-language requirement
 * from the Part 1 design document. Selecting one applies it as the app's UI language
 * via Android's per-app language preferences (AppCompatDelegate.setApplicationLocales),
 * pulling translated strings from the matching values-<code> resource folder.
 *
 * This is a STATIC UI translation approach — it changes labels, buttons, and other
 * app chrome, not user-typed chat message content (that would require a live
 * translation API, which this project deliberately does not use — see README for
 * why Cloud Translation was dropped in favour of this approach).
 *
 * IMPORTANT: the string translations in each values-<code>/strings.xml were produced
 * without access to a professional translation service or a native-speaker review.
 * Treat them as a best-effort placeholder good enough to demonstrate the mechanism
 * working end-to-end, and have a native speaker check them before relying on their
 * accuracy for a real audience. Afrikaans, isiZulu, isiXhosa, Sesotho and Setswana are
 * reasonably well-established; Xitsonga, siSwati, isiNdebele and especially Tshivenda
 * are lower-confidence and should be prioritised for review.
 */
object SupportedLanguages {
    val ALL = listOf(
        Language("en", "English"),
        Language("af", "Afrikaans"),
        Language("zu", "isiZulu"),
        Language("xh", "isiXhosa"),
        Language("st", "Sesotho"),
        Language("tn", "Setswana"),
        Language("ts", "Xitsonga"),
        Language("ss", "siSwati"),
        Language("ve", "Tshivenda"),
        Language("nr", "isiNdebele"),
        Language("nso", "Sepedi")
    )

    fun byCode(code: String): Language = ALL.firstOrNull { it.code == code } ?: ALL.first()
}
