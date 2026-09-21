package com.connecthub.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Note: Validators.isValidEmail uses android.util.Patterns, which requires
 * either Robolectric or running as an instrumented test on a real/emulated
 * device, since the plain JVM unit test environment has no Android framework.
 * Password/name/message validators below are pure Kotlin and run fine as
 * plain JUnit tests.
 */
class ValidatorsTest {

    @Test
    fun `password shorter than 6 chars is invalid`() {
        assertFalse(Validators.isValidPassword("12345"))
    }

    @Test
    fun `password of exactly 6 chars is valid`() {
        assertTrue(Validators.isValidPassword("123456"))
    }

    @Test
    fun `blank display name is invalid`() {
        assertFalse(Validators.isValidDisplayName("   "))
    }

    @Test
    fun `display name over 50 chars is invalid`() {
        val longName = "a".repeat(51)
        assertFalse(Validators.isValidDisplayName(longName))
    }

    @Test
    fun `display name within limit is valid`() {
        assertTrue(Validators.isValidDisplayName("Thapelo Mohale"))
    }

    @Test
    fun `blank message content is invalid`() {
        assertFalse(Validators.isValidMessageContent(""))
    }

    @Test
    fun `non-blank message content within limit is valid`() {
        assertTrue(Validators.isValidMessageContent("Hello there"))
    }

    @Test
    fun `validateRegistration fails when password too short`() {
        val result = Validators.validateRegistration("Thapelo", "test@example.com", "123")
        assertFalse(result.isValid)
        assertEquals("Password must be at least 6 characters.", result.errorMessage)
    }

    @Test
    fun `validateLogin fails when password blank`() {
        val result = Validators.validateLogin("test@example.com", "")
        assertFalse(result.isValid)
        assertEquals("Enter your password.", result.errorMessage)
    }
}
