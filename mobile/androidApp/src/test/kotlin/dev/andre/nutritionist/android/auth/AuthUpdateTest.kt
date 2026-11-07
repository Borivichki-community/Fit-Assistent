package dev.andre.nutritionist.android.auth

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuthUpdateTest {

    private val update = AuthUpdate()

    @Test
    fun `StartLogin transitions state to Loading and issues LaunchAuthTab command`() {
        val initialState = AuthState(content = AuthState.Content.Idle)

        val next = update.update(initialState, AuthEvent.Ui.StartLogin)

        assertEquals(AuthState.Content.Loading, next.state?.content)
        assertEquals(listOf(AuthCommand.LaunchAuthTab), next.commands)
        assertTrue(next.news.isEmpty())
    }

    @Test
    fun `AuthResponseReceived transitions state to Loading and issues ProcessAuthResponse command`() {
        val initialState = AuthState(content = AuthState.Content.Idle)
        val testIntent = Intent("test.action")

        val next = update.update(initialState, AuthEvent.Ui.AuthResponseReceived(testIntent))

        assertEquals(AuthState.Content.Loading, next.state?.content)
        assertEquals(listOf(AuthCommand.ProcessAuthResponse(testIntent)), next.commands)
    }

    @Test
    fun `AuthSuccess transitions state to Authorized and emits NavigateToMain news`() {
        val initialState = AuthState(content = AuthState.Content.Loading)

        val next = update.update(initialState, AuthEvent.Internal.AuthSuccess)

        assertEquals(AuthState.Content.Authorized, next.state?.content)
        assertEquals(listOf(AuthNews.NavigateToMain), next.news)
        assertTrue(next.commands.isEmpty())
    }

    @Test
    fun `AuthFailed transitions state to Error and emits ShowSnackbar news`() {
        val initialState = AuthState(content = AuthState.Content.Loading)
        val error = RuntimeException("Keycloak connection refused")

        val next = update.update(initialState, AuthEvent.Internal.AuthFailed(error))

        assertEquals(AuthState.Content.Error("Keycloak connection refused"), next.state?.content)
        assertEquals(listOf(AuthNews.ShowSnackbar("Keycloak connection refused")), next.news)
    }

    @Test
    fun `LogoutClicked transitions state to Loading and issues Logout command`() {
        val initialState = AuthState(content = AuthState.Content.Authorized)

        val next = update.update(initialState, AuthEvent.Ui.LogoutClicked)

        assertEquals(AuthState.Content.Loading, next.state?.content)
        assertEquals(listOf(AuthCommand.Logout), next.commands)
    }

    @Test
    fun `LoggedOut transitions state to Idle`() {
        val initialState = AuthState(content = AuthState.Content.Loading)

        val next = update.update(initialState, AuthEvent.Internal.LoggedOut)

        assertEquals(AuthState.Content.Idle, next.state?.content)
        assertTrue(next.commands.isEmpty())
        assertTrue(next.news.isEmpty())
    }
}
