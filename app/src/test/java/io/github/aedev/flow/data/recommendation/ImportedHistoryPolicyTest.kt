package io.github.aedev.flow.data.recommendation

import com.google.common.truth.Truth.assertThat
import io.github.aedev.flow.data.model.Video
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test

class ImportedHistoryPolicyTest {
    private val now = System.currentTimeMillis()

    private fun video(
        id: String,
        ageDays: Int = 0,
        short: Boolean = false,
    ) = Video(
        id,
        "Guitar fingerstyle lesson $id",
        "Lessons",
        "",
        "",
        0,
        0,
        uploadDate = "",
        timestamp = now - ageDays * 86_400_000L,
        isShort = short,
    )

    private fun engine(): FlowNeuroEngine {
        val storage = mockk<NeuroStorage>(relaxed = true) { coEvery { load() } returns UserBrain() }
        return FlowNeuroEngine(mockk(relaxed = true), storage, mockk(relaxed = true))
    }

    @Test
    fun `overlapping exports do not repeatedly train old watches`() =
        runTest {
            val engine = engine()
            engine.bootstrapFromWatchHistory(listOf(video("a"), video("b")))
            val before = engine.getBrainSnapshot()
            engine.bootstrapFromWatchHistory(listOf(video("a"), video("b")))
            val after = engine.getBrainSnapshot()
            assertThat(after.globalVector).isEqualTo(before.globalVector)
            assertThat(after.totalInteractions).isEqualTo(before.totalInteractions)
            assertThat(after.channelScores).doesNotContainKey("")
        }

    @Test
    fun `recent history outweighs a years old import`() {
        assertThat(importedHistoryWeight(now - 365 * 86_400_000L, now)).isLessThan(importedHistoryWeight(now, now) / 4)
        assertThat(importedHistoryWeight(0, now)).isAtMost(0.1)
    }

    @Test
    fun `imported unknown progress can retrieve related content without becoming a full watch`() {
        val imported = ImportedWatchSeed("takeout", "Guitar lesson", "", now).graphSeed()
        assertThat(GraphSeedSelector.select(listOf(imported), 2, now)).containsExactly("takeout")
        assertThat(imported.percentWatched).isEqualTo(0.0)
        assertThat(GraphSeedSelector.scoreSeed(imported, now)).isLessThan(0.5)
        assertThat(GraphSeedSelector.select(listOf(imported.copy(unknownWatchProgress = false)), 2, now)).isEmpty()
    }

    @Test
    fun `shorts import teaches the Shorts vector and carries weak seeds into account exports`() =
        runTest {
            val engine = engine()
            engine.bootstrapFromWatchHistory(listOf(video("short", short = true)))
            val brain = engine.getBrainSnapshot()
            assertThat(brain.shortsVector.topics).isNotEmpty()
            assertThat(brain.globalVector.topics).isEmpty()
            assertThat(brain.importedHistorySeeds.single().isShort).isTrue()
            val seed = ShortsSeedInput("short", "", ShortsSeedSource.WATCHED, now, 0.0, unknownWatchProgress = true)
            assertThat(ShortsSeedSelector.select(listOf(seed), 2, now)).containsExactly("short")
        }

    @Test
    fun `recent seeds and reset boundary survive the actual brain export format`() {
        val brain = UserBrain(importedHistorySeeds = listOf(ImportedWatchSeed("id", "Guitar lesson", "", now)), historyResetAt = now)
        val storage = NeuroStorage(mockk(relaxed = true))
        val exported = with(storage) { brain.toSerializable() }
        val restored = Json.decodeFromString<NeuroStorage.SerializableBrain>(Json.encodeToString(exported)).toUserBrain()
        assertThat(restored.importedHistorySeeds).isEqualTo(brain.importedHistorySeeds)
        assertThat(restored.historyResetAt).isEqualTo(now)
    }
}
