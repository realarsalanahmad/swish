package io.github.aedev.flow.ui.screens.home

import com.google.common.truth.Truth.assertThat
import io.github.aedev.flow.data.model.Video
import io.github.aedev.flow.data.recommendation.FeedExclusions
import io.github.aedev.flow.data.recommendation.FlowPersona
import io.github.aedev.flow.data.recommendation.TopicEvidence
import io.github.aedev.flow.data.recommendation.UserBrain
import kotlinx.coroutines.test.runTest
import org.junit.Test

class HomeFeedPersonalizationTest {
    private val now = 1_000_000_000L
    private val taste = FeedTasteProfile(3600, setOf("gaming", "guitar"))

    private fun video(
        id: String,
        title: String = "Gaming strategy $id",
        channel: String = id,
    ) = Video(id, title, channel, channel, "", 600, 100, uploadDate = "1 day ago", timestamp = now - 86_400_000)

    private suspend fun lanes(
        subs: List<Video> = emptyList(),
        discovery: List<Video> = emptyList(),
        seen: Set<String> = emptySet(),
    ) = buildHomeFeedLanes(
        subs,
        discovery,
        emptyList(),
        emptyList(),
        emptyList(),
        emptySet(),
        FeedExclusions.NONE,
        { it in seen },
        taste,
        now,
        3,
        emptyMap(),
        { it },
    )

    @Test
    fun `a shallow refresh never reuses the current cards`() =
        runTest {
            val result = assembleHomeFeed(lanes(discovery = listOf(video("old"), video("new"))), setOf("old"), 0, 100)
            assertThat(result.videos.map { it.id }).containsExactly("new")
        }

    @Test
    fun `subscription scarcity cannot fill a page with followed channels`() =
        runTest {
            val subs = (1..40).map { video("s$it") }
            val result = assembleHomeFeed(lanes(subs), emptySet(), 40, 100, targetSize = 40)
            assertThat(result.videos.size).isAtMost(10)
        }

    @Test
    fun `followed channels discovered through other lanes also obey the cap`() {
        val result =
            blendFeedSources(
                lanes = mapOf(FeedSource.RELATED to (1..20).map { video("v$it", channel = "s$it") }),
                quotas = mapOf(FeedSource.RELATED to 20),
                targetSize = 20,
                subscribedChannelIds = (1..20).mapTo(HashSet()) { "s$it" },
                subscriptionBudget = 4,
            )
        assertThat(result.videos).hasSize(4)
    }

    @Test
    fun `recently seen items leave every feed lane`() =
        runTest {
            val result = lanes(listOf(video("s")), listOf(video("d")), setOf("s", "d"))
            assertThat(result.bestSubs).isEmpty()
            assertThat(result.bestDiscovery).isEmpty()
        }

    @Test
    fun `unsolicited news religion and obvious bait do not fill recommendations`() {
        listOf("Breaking news today", "Spiritual sermon", "Daily bhajan", "AI generated stories", "You won't believe this").forEach {
            assertThat(rejectsHomeCandidate(video(it, it), taste)).isTrue()
        }
        assertThat(rejectsHomeCandidate(video("game", "Gaming strategy"), taste)).isFalse()
        assertThat(rejectsHomeCandidate(video("warbler", "Bird watching warblers"), taste)).isFalse()
    }

    @Test
    fun `subscription inference cannot unlock unwanted genres but genuine watches can`() {
        val inferred = feedTasteProfile(UserBrain(topicAffinities = mapOf("news" to 1.0)), FlowPersona.SKIMMER)
        val watched =
            feedTasteProfile(
                UserBrain(topicEvidence = mapOf("news" to TopicEvidence(watchSignals = 3, positiveScore = 0.8))),
                FlowPersona.SKIMMER,
            )
        val news = video("news", "Breaking news")
        assertThat(rejectsHomeCandidate(news, inferred)).isTrue()
        assertThat(rejectsHomeCandidate(news, watched)).isFalse()
    }

    @Test
    fun `audio lyrics and visualizers stay out of video Home while music videos have a small budget`() =
        runTest {
            listOf("Guitar song official audio", "Song lyrics", "Song visualizer").forEach {
                assertThat(rejectsHomeCandidate(video(it, it), taste)).isTrue()
            }
            assertThat(rejectsHomeCandidate(video("audio", "Song official audio", "ArtistVEVO"), taste)).isTrue()
            assertThat(rejectsHomeCandidate(video("tutorial", "Music theory lesson"), taste)).isFalse()
            val music = (1..20).map { video("m$it", "Artist official music video $it") }
            val normal =
                (1..40).map {
                    video(
                        "n$it",
                        when (it % 3) {
                            0 -> "Cooking recipe $it"
                            1 -> "Coding tutorial $it"
                            else -> "Video review $it"
                        },
                    )
                }
            val result = assembleHomeFeed(lanes(discovery = music + normal), emptySet(), 0, 100, targetSize = 40)
            assertThat(result.videos.count(::isHomeMusic)).isAtMost(4)
            assertThat(result.videos.count { !isHomeMusic(it) }).isAtLeast(20)
        }
}
