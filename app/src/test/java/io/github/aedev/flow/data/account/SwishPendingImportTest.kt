package io.github.aedev.flow.data.account

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

class SwishPendingImportTest {
    private val api = mockk<SwishAccountApi>(relaxed = true)
    private val store = mockk<SwishAccountStore>(relaxed = true)
    private val profiles = mockk<SwishProfileManager>(relaxed = true)
    private val profile = SwishProfile("{\"importedHistoryIds\":[\"aaaaaaaaaaa\"]}", "{}")
    private val session =
        SwishSession(SwishUser("owner", "one@example.com", "One"), "a".repeat(43), Instant.now().plusSeconds(3600).toString())

    @Test
    fun `Takeout imported before signing in is uploaded instead of overwritten by old cloud taste`() =
        runTest {
            coEvery { store.load() } returns null
            coEvery { store.pendingImport() } returns SwishPendingImport(profile)
            coEvery { api.login(any(), any()) } returns session
            val accounts = SwishAccountRepository(api, store, profiles)
            assertThat(accounts.login("one@example.com", "long-password")).isTrue()
            coVerify(exactly = 1) { api.saveProfile(session.token, profile) }
            coVerify(exactly = 0) { api.profile(any()) }
            coVerify(exactly = 1) { profiles.restore(profile) }
            coVerify(exactly = 1) { store.finishImportTracking() }
        }

    @Test
    fun `Takeout on an older install is captured before the first new sign in`() =
        runTest {
            coEvery { store.load() } returns null
            coEvery { profiles.hasImportedHistory() } returns true
            coEvery { profiles.export() } returns profile
            var pending: SwishPendingImport? = null
            coEvery { store.savePendingImport(any()) } coAnswers { pending = firstArg() }
            coEvery { store.pendingImport() } coAnswers { pending }
            coEvery { api.login(any(), any()) } returns session
            val accounts = SwishAccountRepository(api, store, profiles)
            assertThat(accounts.login("one@example.com", "long-password")).isTrue()
            coVerify(exactly = 1) { api.saveProfile(session.token, profile) }
            coVerify(exactly = 0) { api.profile(any()) }
        }

    @Test
    fun `signup claims a pending local Takeout import`() =
        runTest {
            coEvery { store.load() } returns null
            coEvery { store.pendingImport() } returns SwishPendingImport(profile)
            coEvery { api.register(any(), any(), any()) } returns session
            val accounts = SwishAccountRepository(api, store, profiles)
            assertThat(accounts.register("One", "one@example.com", "long-password")).isTrue()
            coVerify(exactly = 1) { api.saveProfile(session.token, profile) }
        }

    @Test
    fun `failed upload retains the pending Takeout and does not restore old cloud data`() =
        runTest {
            coEvery { store.load() } returns null
            coEvery { store.pendingImport() } returns SwishPendingImport(profile)
            coEvery { api.login(any(), any()) } returns session
            coEvery { api.saveProfile(any(), any()) } throws SwishAccountException(SwishAccountFailure.UNAVAILABLE)
            val accounts = SwishAccountRepository(api, store, profiles)
            assertThat(accounts.login("one@example.com", "long-password")).isFalse()
            coVerify(exactly = 0) { store.finishImportTracking() }
            coVerify(exactly = 0) { api.profile(any()) }
            coVerify(exactly = 0) { profiles.restore(any()) }
            coVerify(exactly = 1) { store.savePendingImport(SwishPendingImport(profile, session.user.id)) }
        }

    @Test
    fun `signing out stages an older local import with its existing owner`() =
        runTest {
            coEvery { store.load() } returns session
            coEvery { profiles.hasImportedHistory() } returns true
            coEvery { profiles.export() } returns profile
            val accounts = SwishAccountRepository(api, store, profiles)
            assertThat(accounts.logout()).isTrue()
            coVerify(exactly = 1) { store.savePendingImport(SwishPendingImport(profile, session.user.id)) }
        }

    @Test
    fun `a different account never claims another owners pending Takeout`() =
        runTest {
            coEvery { store.load() } returns null
            coEvery { store.pendingImport() } returns SwishPendingImport(profile, "someone-else")
            coEvery { api.login(any(), any()) } returns session
            coEvery { api.profile(any()) } returns SwishProfile("{}", "{}")
            val accounts = SwishAccountRepository(api, store, profiles)
            assertThat(accounts.login("one@example.com", "long-password")).isTrue()
            coVerify(exactly = 0) { api.saveProfile(any(), any()) }
            coVerify(exactly = 0) { store.finishImportTracking() }
        }

    @Test
    fun `an unsigned import is held locally until a later login`() =
        runTest {
            coEvery { store.load() } returns null
            coEvery { profiles.export() } returns profile
            val accounts = SwishAccountRepository(api, store, profiles)
            assertThat(accounts.takeoutImported(null)).isTrue()
            coVerify(exactly = 1) { store.savePendingImport(SwishPendingImport(profile)) }
            coVerify(exactly = 0) { api.saveProfile(any(), any()) }
        }

    @Test
    fun `reset clears pending Takeout only after replacing the saved profile`() =
        runTest {
            coEvery { store.load() } returns session
            coEvery { profiles.export() } returns SwishProfile("{}", "{}")
            val accounts = SwishAccountRepository(api, store, profiles)
            assertThat(accounts.resetProfile()).isTrue()
            coVerify(exactly = 1) { profiles.reset(true, true) }
            coVerify(exactly = 1) { store.finishImportTracking() }
        }
}
