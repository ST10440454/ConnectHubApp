package com.connecthub.app.util

import android.util.Patterns

/**
 * Pure validation functions. Kept free of Android framework dependencies
 * (except Patterns, which is fine in unit tests via Robolectric or a fake)
 * so they are cheap and fast to unit test.
 */
object Validators {

    fun isValidEmail(email: String): Boolean {
        if (email.isBlank()) return false
        // Basic email validation regex to avoid android.util.Patterns dependency in unit tests
        return Patterns.EMAIL_ADDRESS?.matcher(email)?.matches() ?: email.contains("@")
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    fun isValidDisplayName(name: String): Boolean {
        return name.isNotBlank() && name.length <= 50
    }

    fun isValidMessageContent(content: String): Boolean {
        return content.isNotBlank() && content.length <= 10_000
    }

    data class ValidationResult(val isValid: Boolean, val errorMessage: String? = null)

    fun validateRegistration(name: String, email: String, password: String): ValidationResult {
        return when {
            !isValidDisplayName(name) -> ValidationResult(false, "Enter a display name (max 50 characters).")
            !isValidEmail(email) -> ValidationResult(false, "Enter a valid email address.")
            !isValidPassword(password) -> ValidationResult(false, "Password must be at least 6 characters.")
            else -> ValidationResult(true)
        }
    }

    fun validateLogin(email: String, password: String): ValidationResult {
        return when {
            !isValidEmail(email) -> ValidationResult(false, "Enter a valid email address.")
            password.isBlank() -> ValidationResult(false, "Enter your password.")
            else -> ValidationResult(true)
        }
    }
}
