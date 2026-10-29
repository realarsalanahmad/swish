package io.github.aedev.flow.ui.screens.player

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.stream.AudioStream

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerAudioTrackCatalogTest {
    @Test
    fun aResultForThePreviousVideoIsIgnored() =
        runTest {
            var currentVideo = "first"
            var published = false
            val gate = CompletableDeferred<Unit>()
            val stream =
                AudioStream
                    .Builder()
                    .setId("140")
                    .setContent("https://example.invalid/audio", true)
                    .setMediaFormat(MediaFormat.M4A)
                    .setAverageBitrate(128_000)
                    .build()
            val catalog =
                PlayerAudioTrackCatalog(this, { currentVideo }, {
                    gate.await()
                    listOf(stream)
                }, { _, _ -> published = true })
            catalog.request("first")
            runCurrent()
            currentVideo = "second"
            gate.complete(Unit)
            advanceUntilIdle()
            assertFalse(published)
        }

    @Test
    fun explicitRefreshRunsOnceAfterThePreviousRequestCompletes() =
        runTest {
            var requests = 0
            val catalog =
                PlayerAudioTrackCatalog(this, { "video" }, {
                    requests++
                    emptyList()
                }, { _, _ -> })
            catalog.request("video")
            catalog.request("video", force = true)
            advanceUntilIdle()
            catalog.request("video", force = true)
            advanceUntilIdle()
            assertEquals(2, requests)
        }

    @Test
    fun requestsAreLazyAndDeduplicatedPerVideo() =
        runTest {
            var requests = 0
            val catalog =
                PlayerAudioTrackCatalog(this, { "video" }, {
                    requests++
                    emptyList()
                }, { _, _ -> })
            assertEquals(0, requests)
            catalog.request("video")
            catalog.request("video")
            advanceUntilIdle()
            catalog.request("video")
            advanceUntilIdle()
            assertEquals(1, requests)
        }

    @Test
    fun failedExtractionDoesNotReplaceThePlayableCatalog() =
        runTest {
            var published = false
            val catalog = PlayerAudioTrackCatalog(this, { "video" }, { throw IllegalStateException() }, { _, _ -> published = true })
            catalog.request("video")
            advanceUntilIdle()
            assertFalse(published)
            assertFalse(catalog.loading.value)
        }

    @Test
    fun timeoutClearsLoadingAndKeepsPlaybackUntouched() =
        runTest {
            var published = false
            val catalog =
                PlayerAudioTrackCatalog(this, { "video" }, {
                    delay(60_000)
                    emptyList()
                }, { _, _ -> published = true })
            catalog.request("video")
            runCurrent()
            advanceTimeBy(20_000)
            runCurrent()
            assertFalse(catalog.loading.value)
            assertFalse(published)
        }

    @Test
    fun switchingVideosCancelsThePreviousRequest() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val requests = mutableListOf<String>()
            val catalog =
                PlayerAudioTrackCatalog(this, { "second" }, { id ->
                    requests += id
                    gate.await()
                    emptyList()
                }, { _, _ -> })
            catalog.request("first")
            runCurrent()
            catalog.request("second")
            runCurrent()
            gate.complete(Unit)
            advanceUntilIdle()
            assertEquals(listOf("first", "second"), requests)
            assertFalse(catalog.loading.value)
        }
}
