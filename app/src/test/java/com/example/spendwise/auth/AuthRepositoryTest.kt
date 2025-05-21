package com.example.spendwise.auth

import com.example.spendwise.data.repository.AuthRepository
import com.example.spendwise.data.repository.SecureTokenStorage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.AuthResult as FirebaseAuthResult
import com.google.firebase.auth.AuthCredential
import com.google.android.gms.tasks.Task
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.tasks.await
import org.junit.Before
import org.junit.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryTest {

    private lateinit var mockAuth: FirebaseAuth
    private lateinit var mockTokenStorage: SecureTokenStorage
    private lateinit var repository: AuthRepository

    @Before
    fun setUp() {
        mockAuth = mockk(relaxed = true)
        mockTokenStorage = mockk(relaxed = true)
        repository = AuthRepository(mockAuth, mockTokenStorage)
    }

    @Test
    fun `login returns success when signInWithEmailAndPassword succeeds`() = runTest {
        val email = "test@example.com"
        val password = "password123"
        val mockTask = mockk<Task<FirebaseAuthResult>>(relaxed = true)

        every { mockAuth.signInWithEmailAndPassword(email, password) } returns mockTask
        coEvery { mockTask.await() } returns mockk()

        val result = repository.login(email, password)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `login returns failure when signInWithEmailAndPassword throws`() = runTest {
        val email = "test@example.com"
        val password = "password123"
        val mockTask = mockk<Task<FirebaseAuthResult>>(relaxed = true)

        every { mockAuth.signInWithEmailAndPassword(email, password) } returns mockTask
        coEvery { mockTask.await() } throws Exception("Login failed")

        val result = repository.login(email, password)
        assertFalse(result.isSuccess)
    }

    @Test
    fun `register returns success when createUserWithEmailAndPassword succeeds`() = runTest {
        val email = "test@example.com"
        val password = "password123"
        val mockTask = mockk<Task<FirebaseAuthResult>>(relaxed = true)

        every { mockAuth.createUserWithEmailAndPassword(email, password) } returns mockTask
        coEvery { mockTask.await() } returns mockk()

        val result = repository.register(email, password)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `register returns failure when createUserWithEmailAndPassword throws`() = runTest {
        val email = "test@example.com"
        val password = "password123"
        val mockTask = mockk<Task<FirebaseAuthResult>>(relaxed = true)

        every { mockAuth.createUserWithEmailAndPassword(email, password) } returns mockTask
        coEvery { mockTask.await() } throws Exception("Registration failed")

        val result = repository.register(email, password)
        assertFalse(result.isSuccess)
    }

    @Test
    fun `loginWithGoogle returns success when signInWithCredential succeeds`() = runTest {
        val idToken = "fake_id_token"
        val credential = mockk<AuthCredential>()
        val mockTask = mockk<Task<FirebaseAuthResult>>(relaxed = true)

        mockkStatic("com.google.firebase.auth.GoogleAuthProvider")
        every { com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null) } returns credential
        every { mockAuth.signInWithCredential(credential) } returns mockTask
        coEvery { mockTask.await() } returns mockk()

        val result = repository.loginWithGoogle(idToken)
        assertTrue(result.isSuccess)
        unmockkStatic("com.google.firebase.auth.GoogleAuthProvider")
    }

    @Test
    fun `loginWithGoogle returns failure when signInWithCredential throws`() = runTest {
        val idToken = "fake_id_token"
        val credential = mockk<AuthCredential>()
        val mockTask = mockk<Task<FirebaseAuthResult>>(relaxed = true)

        mockkStatic("com.google.firebase.auth.GoogleAuthProvider")
        every { com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null) } returns credential
        every { mockAuth.signInWithCredential(credential) } returns mockTask
        coEvery { mockTask.await() } throws Exception("Google login failed")

        val result = repository.loginWithGoogle(idToken)
        assertFalse(result.isSuccess)
        unmockkStatic("com.google.firebase.auth.GoogleAuthProvider")
    }
} 