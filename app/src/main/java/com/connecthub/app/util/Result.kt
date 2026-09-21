package com.connecthub.app.util

/**
 * Wraps outcomes of repository/use-case calls so ViewModels never
 * have to catch raw Firebase exceptions directly.
 */
sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Error(val message: String) : AppResult<Nothing>()
}

/** Runs [block], converting any thrown exception into a friendly [AppResult.Error]. */
inline fun <T> runCatchingApp(block: () -> T): AppResult<T> {
    return try {
        AppResult.Success(block())
    } catch (e: Exception) {
        AppResult.Error(e.toFriendlyMessage())
    }
}

fun Exception.toFriendlyMessage(): String {
    val raw = this.message ?: return "Something went wrong. Please try again."
    return when {
        raw.contains("email address is already in use", ignoreCase = true) ->
            "An account with that email already exists. Try logging in instead."
        raw.contains("password is invalid", ignoreCase = true) ||
            raw.contains("no user record", ignoreCase = true) ->
            "Incorrect email or password."
        raw.contains("badly formatted", ignoreCase = true) ->
            "That email address doesn't look valid."
        raw.contains("network error", ignoreCase = true) ->
            "No internet connection. Check your network and try again."
        raw.contains("weak password", ignoreCase = true) ->
            "Password should be at least 6 characters."
        else -> raw
    }
}
