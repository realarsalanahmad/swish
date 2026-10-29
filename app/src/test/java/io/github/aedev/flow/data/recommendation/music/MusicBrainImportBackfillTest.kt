package io.github.aedev.flow.data.recommendation.music

import com.google.common.truth.Truth.assertThat
import io.github.aedev.flow.data.local.dao.WatchHistoryDao
import io.github.aedev.flow.data.local.entity.WatchHistoryEntity
import io.github.aedev.flow.data.music.PlaylistRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MusicBrainImportBackfillTest {
    @Test fun `an unknown historical date does not become a listen today`() =
        runTest {
            val dao = mockk<WatchHistoryDao>()
            val playlists = mockk<PlaylistRepository>()
            every { playlists.history } returns flowOf(emptyList())
            coEvery { dao.getHistoryPage(any(), any(), any(), any(), any()) } returns listOf(row("unknown", 0))
            val brain = MusicBrain()
            MusicBrainBackfill(dao, playlists).run(brain, now, onlyMissingTracks = true)
            assertThat(brain.trackPlays["unknown"]).containsExactly(0L)
            assertThat(brain.trackMeta).containsKey("unknown")
        }

    private val now = 1_700_000_000_000L

    @Test fun `newly imported songs warm an initialized brain without recounting existing tracks`() =
        runTest {
            val dao = mockk<WatchHistoryDao>()
            val playlists = mockk<PlaylistRepository>()
            every { playlists.history } returns flowOf(emptyList())
            coEvery { dao.getHistoryPage(any(), any(), any(), any(), any()) } returns listOf(row("new", now - 10), row("held", now - 20))
            val brain = MusicBrain()
            brain.trackPlays["held"] = mutableListOf(now - 20)
            brain.totalPlays = 1
            brain.discoveryAppetite = 0.4
            val backfill = MusicBrainBackfill(dao, playlists)
            backfill.run(brain, now, onlyMissingTracks = true)
            assertThat(brain.totalPlays).isEqualTo(2)
            assertThat(brain.trackPlays["held"]).containsExactly(now - 20)
            assertThat(brain.trackPlays["new"]).containsExactly(now - 10)
            assertThat(brain.discoveryAppetite).isEqualTo(0.4)
            backfill.run(brain, now, onlyMissingTracks = true)
            assertThat(brain.totalPlays).isEqualTo(2)
        }

    private fun row(
        id: String,
        at: Long,
    ) = WatchHistoryEntity(id, 0, 0, at, "Song", "", "Artist", "", true)
}
