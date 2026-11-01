package com.connecthub.app.presentation.common

import android.content.Context
import androidx.annotation.StringRes
import com.connecthub.app.ConnectHubApp
import com.connecthub.app.data.i18n.LanguageManager

/** The app-wide [LanguageManager]. */
val Context.languageManager: LanguageManager
    get() = (applicationContext as ConnectHubApp).container.languageManager

/** Like getString(), but returns the text in the language chosen in Settings. */
fun Context.tr(@StringRes id: Int, vararg args: Any): String = languageManager.tr(id, *args)
