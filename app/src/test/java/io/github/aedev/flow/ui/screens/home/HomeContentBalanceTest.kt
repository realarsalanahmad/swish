package io.github.aedev.flow.ui.screens.home

import com.google.common.truth.Truth.assertThat
import io.github.aedev.flow.data.model.Video
import org.junit.Test

class HomeContentBalanceTest {
    private fun video(
        id: Int,
        title: String,
    ) = Video("$id", title, "Channel $id", "channel$id", "", 600, 100, uploadDate = "")

    @Test
    fun `a page cannot be flooded by one format across different channels`() {
        val candidates =
            (1..40).map { video(it, "Minecraft gameplay") } + (41..60).map { video(it, "Cooking recipe") } +
                (61..80).map { video(it, "Programming tutorial") }
        val page = blendFeedSources(mapOf(FeedSource.RELATED to candidates), mapOf(FeedSource.RELATED to 40), 40).videos
        assertThat(page).hasSize(40)
        assertThat(
            page
                .groupingBy(::homeContentFamily)
                .eachCount()
                .values
                .max(),
        ).isAtMost(16)
    }

    @Test
    fun `unwanted horror is blocked without blocking ordinary storytelling or AI education`() {
        val taste = FeedTasteProfile(3600, setOf("programming", "gaming"))
        assertThat(rejectsHomeCandidate(video(1, "Haunted house horror story"), taste)).isTrue()
        assertThat(rejectsHomeCandidate(video(2, "AI generated horror stories"), taste)).isTrue()
        assertThat(rejectsHomeCandidate(video(3, "How AI image generation works"), taste)).isFalse()
        assertThat(rejectsHomeCandidate(video(4, "The story of a programming language"), taste)).isFalse()
    }
}
