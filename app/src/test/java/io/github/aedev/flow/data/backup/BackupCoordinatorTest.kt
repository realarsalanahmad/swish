package io.github.aedev.flow.data.backup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.google.common.truth.Truth.assertThat
import io.github.aedev.flow.R
import io.github.aedev.flow.data.account.SwishAccountRepository
import io.github.aedev.flow.data.account.SwishAccountState
import io.github.aedev.flow.data.account.SwishUser
import io.github.aedev.flow.data.local.BackupRepository
import io.github.aedev.flow.data.local.PlayerPreferences
import io.github.aedev.flow.data.recommendation.music.FavouriteArtistsStore
import io.github.aedev.flow.data.recommendation.music.MusicBrainEngine
import io.github.aedev.flow.notification.NotificationHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

class BackupCoordinatorTest {
    private val context: Context = mockk(relaxed = true)
    private val repository: BackupRepository = mockk(relaxed = true)
    private val first: Uri = mockk()
    private val second: Uri = mockk()
    private val favourites: FavouriteArtistsStore = mockk(relaxed = true)
    private val musicBrain: MusicBrainEngine = mockk(relaxed = true)
    private val accounts: SwishAccountRepository = mockk(relaxed = true)
    private val preferences: PlayerPreferences = mockk(relaxed = true)
    private val accountState = MutableStateFlow(SwishAccountState(initialized = true))

    private fun coordinator() =
        BackupCoordinator(
            context,
            repository,
            musicBrain,
            mockk(relaxed = true),
            mockk(relaxed = true),
            favourites,
            mockk(relaxed = true),
            accounts,
            preferences,
        )

    @Before
    fun setUp() {
        every { accounts.state } returns accountState
        mockkStatic(DocumentsContract::class)
        every { DocumentsContract.deleteDocument(any(), any()) } returns true
        mockkObject(NotificationHelper)
        every { NotificationHelper.cancelImportNotification(any()) } returns Unit
        every { NotificationHelper.hasNotificationPermission(any()) } returns false
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `an export started while another runs deletes the file the save dialog created`() {
        val gate = CompletableDeferred<Result<Unit>>()
        coEvery { repository.exportData(first) } coAnswers { gate.await() }
        val coordinator = coordinator()

        assertThat(coordinator.exportAppData(first)).isTrue()
        assertThat(coordinator.exportNewPipeSubscriptions(second)).isFalse()

        verify(timeout = 2_000) { DocumentsContract.deleteDocument(any(), second) }
        verify(exactly = 0) { DocumentsContract.deleteDocument(any(), first) }
        gate.cancel()
    }

    @Test
    fun `a failed export deletes its empty file and reports the failure`() =
        runBlocking {
            coEvery { repository.exportSubscriptionsAsNewPipe(first) } returns Result.failure(IllegalStateException())
            val coordinator = coordinator()

            assertThat(coordinator.exportNewPipeSubscriptions(first)).isTrue()

            val outcome = withTimeout(2_000) { coordinator.operation.first { it !is BackupOperation.Running } }
            assertThat(outcome).isInstanceOf(BackupOperation.Failed::class.java)
            verify { DocumentsContract.deleteDocument(any(), first) }
        }

    @Test
    fun `a successful export keeps its file`() =
        runBlocking {
            coEvery { repository.exportSubscriptionsAsNewPipe(first) } returns Result.success(NewPipeSubscriptionExport("{}", skipped = 0))
            val coordinator = coordinator()

            coordinator.exportNewPipeSubscriptions(first)

            val outcome = withTimeout(2_000) { coordinator.operation.first { it !is BackupOperation.Running } }
            assertThat(outcome).isInstanceOf(BackupOperation.Succeeded::class.java)
            verify(exactly = 0) { DocumentsContract.deleteDocument(any(), any()) }
        }

    @Test
    fun `the master backup carries the picked artists and restores them`() =
        runBlocking {
            val picks = "[]".toByteArray()
            coEvery { favourites.export() } returns picks
            coEvery { repository.exportMasterBackup(first, any(), any(), picks) } returns Result.success(Unit)
            val restore = slot<suspend (ByteArray) -> Unit>()
            coEvery { repository.importMasterBackup(second, any(), any(), capture(restore)) } coAnswers {
                restore.captured(picks)
                Result.success(Unit)
            }
            val coordinator = coordinator()

            coordinator.exportMaster(first)
            withTimeout(2_000) { coordinator.operation.first { it is BackupOperation.Succeeded } }
            coordinator.dismiss()
            coordinator.importMaster(second)
            withTimeout(2_000) { coordinator.operation.first { it is BackupOperation.Succeeded } }

            coVerify { repository.exportMasterBackup(first, any(), any(), picks) }
            coVerify { favourites.restore(picks) }
        }

    @Test
    fun `the outcome is published after the last notification call`() =
        runBlocking {
            coEvery { repository.importMasterBackup(second, any(), any(), any()) } returns Result.success(Unit)
            val coordinator = coordinator()
            val seen = CopyOnWriteArrayList<BackupOperation>()
            every { NotificationHelper.hasNotificationPermission(any()) } answers {
                seen += coordinator.operation.value
                false
            }

            coordinator.importMaster(second)
            withTimeout(2_000) { coordinator.operation.first { it is BackupOperation.Succeeded } }

            assertThat(seen).isNotEmpty()
            assertThat(seen.filterIsInstance<BackupOperation.Succeeded>()).isEmpty()
        }

    @Test
    fun `Takeout stays imported when cloud save fails and explains the failure`() =
        runBlocking {
            accountState.value = SwishAccountState(initialized = true, user = SwishUser("first-user", "one@example.com", "One"))
            coEvery { repository.importYouTubeTakeout(listOf(first, second), any()) } returns Result.success("Imported history")
            coEvery { accounts.takeoutImported("first-user") } returns false
            every { context.getString(R.string.swish_takeout_cloud_failed) } returns "Cloud save failed"
            val coordinator = coordinator()

            coordinator.importYouTubeTakeout(listOf(first, second))
            val outcome = withTimeout(2_000) { coordinator.operation.first { it is BackupOperation.Succeeded } }

            assertThat((outcome as BackupOperation.Succeeded).message).isEqualTo("Imported history\nCloud save failed")
            coVerify(exactly = 1) { musicBrain.bootstrapImportedHistory() }
            coVerify(exactly = 1) { preferences.setHideWatchedVideosFromHome(true) }
        }

    @Test
    fun `an account switch during Takeout cannot save history to the replacement account`() =
        runBlocking {
            accountState.value = SwishAccountState(initialized = true, user = SwishUser("first-user", "one@example.com", "One"))
            coEvery { repository.importYouTubeTakeout(listOf(first), any()) } coAnswers {
                accountState.value = SwishAccountState(initialized = true, user = SwishUser("second-user", "two@example.com", "Two"))
                Result.success("Imported history")
            }
            coEvery { accounts.takeoutImported("first-user") } returns null
            val coordinator = coordinator()

            coordinator.importYouTubeTakeout(listOf(first))
            withTimeout(2_000) { coordinator.operation.first { it is BackupOperation.Succeeded } }

            coVerify(exactly = 1) { accounts.takeoutImported("first-user") }
            coVerify(exactly = 0) { accounts.takeoutImported("second-user") }
        }
}
