package com.connecthub.app.domain.usecase

import com.connecthub.app.util.AppResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthUseCasesTest {

    private lateinit var fakeRepository: FakeAuthRepository
    private lateinit var registerUseCase: RegisterUserUseCase
    private lateinit var loginUseCase: LoginUserUseCase

    @Before
    fun setUp() {
        fakeRepository = FakeAuthRepository()
        registerUseCase = RegisterUserUseCase(fakeRepository)
        loginUseCase = LoginUserUseCase(fakeRepository)
    }

    @Test
    fun `register fails validation before hitting repository when password too short`() = runTest {
        val result = registerUseCase("Thapelo", "test@example.com", "123")
        assertTrue(result is AppResult.Error)
        assertEquals("Password must be at least 6 characters.", (result as AppResult.Error).message)
    }

    @Test
    fun `register succeeds with valid input`() = runTest {
        val result = registerUseCase("Thapelo", "test@example.com", "password123")
        assertTrue(result is AppResult.Success)
        val user = (result as AppResult.Success).data
        assertEquals("Thapelo", user.displayName)
        assertEquals("test@example.com", user.email)
    }

    @Test
    fun `register propagates repository failure`() = runTest {
        fakeRepository.shouldFail = true
        fakeRepository.failureMessage = "Email already in use."
        val result = registerUseCase("Thapelo", "test@example.com", "password123")
        assertTrue(result is AppResult.Error)
        assertEquals("Email already in use.", (result as AppResult.Error).message)
    }

    @Test
    fun `login fails validation when password blank`() = runTest {
        val result = loginUseCase("test@example.com", "")
        assertTrue(result is AppResult.Error)
        assertEquals("Enter your password.", (result as AppResult.Error).message)
    }

    @Test
    fun `login succeeds with valid credentials`() = runTest {
        val result = loginUseCase("test@example.com", "password123")
        assertTrue(result is AppResult.Success)
    }
}
