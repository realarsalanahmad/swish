package io.github.aedev.flow.ui.screens.player

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.schabi.newpipe.extractor.stream.AudioStream

internal class PlayerAudioTrackCatalog(
    private val scope: CoroutineScope,
    private val currentVideoId: () -> String?,
    private val extract: suspend (String) -> List<AudioStream>,
    private val publish: (String, List<AudioStream>) -> Unit,
) {
    private var attemptedVideoId: String? = null
    private var work: Job? = null
    private val mutableLoading = MutableStateFlow(false)
    val loading = mutableLoading.asStateFlow()

    fun request(
        videoId: String,
        force: Boolean = false,
    ) {
        if (videoId == attemptedVideoId && (!force || work?.isActive == true)) return
        work?.cancel()
        attemptedVideoId = videoId
        work =
            scope.launch {
                mutableLoading.value = true
                try {
                    val streams = withTimeoutOrNull(20_000) { extract(videoId) }.orEmpty()
                    if (currentVideoId() == videoId && streams.isNotEmpty()) publish(videoId, streams)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // Keep the currently playable tracks if the richer client cannot return a catalog.
                } finally {
                    if (attemptedVideoId == videoId) mutableLoading.value = false
                }
            }
    }
}
