package io.github.aedev.flow.ui.screens.home

import com.google.common.truth.Truth.assertThat
import io.github.aedev.flow.data.model.Video
import org.junit.Test

class HomeFeedFreshnessTest {
    private val now = 200L * 86_400_000

    private fun video(
        id: String,
        days: Int,
        channel: String = "daily",
    ) = Video(
        id,
        "Video $id",
        channel,
        channel,
        "",
        600,
        100,
        uploadDate = "$days days ago",
        timestamp = now - days * 86_400_000L,
        timestampIsExact = true,
    )

    @Test
    fun `daily posters give recent videos a stronger freshness advantage`() {
        val daily = channelFreshnessScores(listOf(video("d1", 1), video("d2", 2), video("d3", 3), video("d30", 30)), now)
        val monthly = channelFreshnessScores(listOf(video("m1", 1), video("m30", 30), video("m60", 60)), now)
        assertThat(daily.getValue("d1") - daily.getValue("d30")).isGreaterThan(monthly.getValue("m1") - monthly.getValue("m30"))
    }

    @Test
    fun `recent content beats equally relevant old uploads`() {
        val scores = channelFreshnessScores(listOf(video("recent", 1), video("archive", 120)), now)
        assertThat(scores.getValue("recent")).isGreaterThan(scores.getValue("archive"))
    }

    @Test
    fun `a default timestamp with no publish metadata is not treated as a new upload`() {
        val undated = video("unknown", 0).copy(timestampIsExact = false, uploadDate = "")
        assertThat(channelFreshnessScores(listOf(undated), now)).isEmpty()
    }
}
