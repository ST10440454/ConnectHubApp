package com.connecthub.app.util

private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$")

/**
 * Pure validation functions. Pure JVM with no Android dependencies
 * so they are cheap and fast to unit test without stubbing/mocking framework classes.
 */
object Validators {

    fun isValidEmail(email: String): Boolean {
        if (email.isBlank()) return false
        return EMAIL_REGEX.matches(email.trim())
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
