package io.github.aedev.flow.data.local

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TakeoutHistorySeedsTest {
    @Test fun `newest entries win even when the archive is oldest first`() {
        val seeds = TakeoutHistorySeeds(2)
        (1L..10L).forEach { seeds.offer(entry("v$it", it)) }
        assertThat(seeds.values.map { it.videoId }).containsExactly("v10", "v9").inOrder()
    }

    @Test fun `overlapping exports keep the newest timestamp per video`() {
        val seeds = TakeoutHistorySeeds(2)
        seeds.offer(entry("same", 10))
        seeds.offer(entry("other", 20))
        seeds.offer(entry("same", 30))
        seeds.offer(entry("same", 5))
        assertThat(seeds.values.map { it.videoId }).containsExactly("same", "other").inOrder()
        assertThat(seeds.values.first().timestamp).isEqualTo(30)
    }

    @Test fun `large music history cannot displace video interests`() {
        val seeds = TakeoutHistorySeeds(2)
        seeds.offer(entry("video", 1))
        (2L..10L).forEach { seeds.offer(entry("song$it", it, true)) }
        assertThat(seeds.values.filterNot { it.isMusic }.map { it.videoId }).containsExactly("video")
        assertThat(seeds.values.filter { it.isMusic }.map { it.videoId }).containsExactly("song10", "song9").inOrder()
    }

    @Test fun `empty metadata and late old rows do not consume the budget`() {
        val seeds = TakeoutHistorySeeds(1)
        seeds.offer(entry("new", 100))
        seeds.offer(entry("old", 1))
        seeds.offer(entry("", 200))
        seeds.offer(entry("blank-title", 200).copy(title = ""))
        assertThat(seeds.values.map { it.videoId }).containsExactly("new")
    }

    private fun entry(
        id: String,
        at: Long,
        music: Boolean = false,
    ) = VideoHistoryEntry(
        videoId = id,
        position = 0,
        duration = 0,
        timestamp = at,
        title = id,
        thumbnailUrl = "",
        isMusic = music,
    )
}
