package io.github.aedev.flow.data.account

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

class SwishAccountRepositoryTest {
    @Test
    fun `login restores saved personalization without another button press`() =
        runTest {
            coEvery { store.load() } returns null
            coEvery { api.login(any(), any()) } returns session
            val profile = SwishProfile("{}", "{}")
            coEvery { api.profile(session.token) } returns profile
            val accounts = SwishAccountRepository(api, store, profiles)
            assertThat(accounts.login("one@example.com", "long-password")).isTrue()
            coVerify(exactly = 1) { profiles.restore(profile) }
            assertThat(accounts.state.value.notice).isEqualTo(SwishAccountNotice.PROFILE_RESTORED)
        }

    @Test
    fun `a missing cloud profile does not prevent first login`() =
        runTest {
            coEvery { store.load() } returns null
            coEvery { api.login(any(), any()) } returns session
            coEvery { api.profile(any()) } throws SwishAccountException(SwishAccountFailure.PROFILE_MISSING)
            val accounts = SwishAccountRepository(api, store, profiles)
            assertThat(accounts.login("one@example.com", "long-password")).isTrue()
            assertThat(accounts.state.value.user).isEqualTo(session.user)
        }

    @Test
    fun `reset replaces the saved account profile and invalidates cached feeds`() =
        runTest {
            coEvery { store.load() } returns session
            val profile = SwishProfile("{}", "{}")
            coEvery { profiles.export() } returns profile
            val accounts = SwishAccountRepository(api, store, profiles)
            assertThat(accounts.resetProfile()).isTrue()
            coVerify(exactly = 1) { profiles.reset(true, true) }
            coVerify(exactly = 1) { api.saveProfile(session.token, profile) }
            coVerify(exactly = 1) { profiles.invalidate() }
            assertThat(accounts.state.value.user).isEqualTo(session.user)
        }

    @Test
    fun `failed cloud reset restores the previous local personalization`() =
        runTest {
            coEvery { store.load() } returns session
            val previous = SwishProfile("{}", "{}")
            coEvery { profiles.export() } returns previous
            coEvery { api.saveProfile(any(), any()) } throws SwishAccountException(SwishAccountFailure.UNAVAILABLE)
            val accounts = SwishAccountRepository(api, store, profiles)
            assertThat(accounts.resetProfile()).isFalse()
            coVerify(exactly = 1) { profiles.restore(previous) }
            assertThat(accounts.state.value.failure).isEqualTo(SwishAccountFailure.UNAVAILABLE)
        }

    private val api = mockk<SwishAccountApi>(relaxed = true)
    private val store = mockk<SwishAccountStore>(relaxed = true)
    private val profiles = mockk<SwishProfileManager>(relaxed = true)
    private val session =
        SwishSession(
            SwishUser("first-user", "one@example.com", "One"),
            "a".repeat(43),
            Instant.now().plusSeconds(3600).toString(),
        )

    @Test
    fun `login normalizes the email and persists the session before publishing the user`() =
        runTest {
            coEvery { store.load() } returns null
            coEvery { api.login("one@example.com", "long-password") } returns session
            val accounts = SwishAccountRepository(api, store, profiles)

            assertThat(accounts.login("  ONE@example.com  ", "long-password")).isTrue()

            coVerify(exactly = 1) { store.save(session) }
            assertThat(accounts.state.value.user).isEqualTo(session.user)
            assertThat(accounts.state.value.busy).isFalse()
            assertThat(accounts.state.value.failure).isNull()
        }

    @Test
    fun `a failed login exposes the actual credential error and clears the busy state`() =
        runTest {
            coEvery { store.load() } returns null
            coEvery { api.login(any(), any()) } throws SwishAccountException(SwishAccountFailure.INVALID_CREDENTIALS)
            val accounts = SwishAccountRepository(api, store, profiles)

            assertThat(accounts.login("one@example.com", "wrong-password")).isFalse()

            assertThat(accounts.state.value.failure).isEqualTo(SwishAccountFailure.INVALID_CREDENTIALS)
            assertThat(accounts.state.value.busy).isFalse()
            assertThat(accounts.state.value.user).isNull()
            coVerify(exactly = 0) { store.save(any()) }
        }

    @Test
    fun `Takeout for a previous account never exports or uploads the current profile`() =
        runTest {
            coEvery { store.load() } returns session.copy(user = SwishUser("second-user", "two@example.com", "Two"))
            val accounts = SwishAccountRepository(api, store, profiles)

            assertThat(accounts.saveProfile("first-user")).isNull()

            coVerify(exactly = 0) { profiles.export() }
            coVerify(exactly = 0) { api.saveProfile(any(), any()) }
        }
}
