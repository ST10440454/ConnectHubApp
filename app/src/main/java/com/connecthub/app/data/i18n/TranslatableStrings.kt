package com.connecthub.app.data.i18n

import android.content.Context
import com.connecthub.app.R

/**
 * Every UI string that is sent to DeepL. When you add a string to values/strings.xml that
 * users should see translated, add its id here too. app_name is deliberately absent: the
 * brand name and launcher label stay as-is.
 */
object TranslatableStrings {
    val IDS: List<Int> = listOf(
        R.string.onboarding_title, R.string.onboarding_subtitle, R.string.action_get_started,
        R.string.login_title, R.string.hint_email, R.string.hint_password, R.string.action_login,
        R.string.prompt_no_account,
        R.string.register_title, R.string.hint_display_name, R.string.hint_password_hint_min,
        R.string.hint_phone_optional, R.string.hint_phone_helper, R.string.action_register,
        R.string.prompt_have_account,
        R.string.hint_write_message, R.string.action_send, R.string.action_attach_image,
        R.string.content_description_shared_image, R.string.empty_contacts_title,
        R.string.empty_contacts_subtitle,
        R.string.new_chat_title, R.string.on_connecthub, R.string.action_chat, R.string.action_invite,
        R.string.contacts_permission_denied_title, R.string.contacts_permission_denied_subtitle,
        R.string.action_open_app_settings, R.string.invite_message_body,
        R.string.action_settings, R.string.label_dark_mode, R.string.label_notifications,
        R.string.label_language, R.string.action_logout, R.string.signed_in_as,
        R.string.translation_failed
    )

    /** The English source texts (the app ships only English resources). */
    fun englishTexts(context: Context): List<String> = IDS.map { context.getString(it) }.distinct()
}
