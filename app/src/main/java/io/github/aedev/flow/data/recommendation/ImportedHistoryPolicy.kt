package io.github.aedev.flow.data.recommendation

import io.github.aedev.flow.data.model.Video
import kotlinx.serialization.Serializable
import kotlin.math.exp

@Serializable
data class ImportedWatchSeed(
    val id: String,
    val title: String,
    val channelId: String,
    val watchedAt: Long,
    val isShort: Boolean = false,
) {
    fun graphSeed() =
        GraphSeedInput(id, title, channelId, GraphSeedSource.WATCH_HISTORY, 0.35, watchedAt, 0, 0.0, unknownWatchProgress = true)
}

internal fun recentImportedHistory(
    videos: List<Video>,
    limit: Int = 500,
): List<Video> {
    val channels = HashMap<String, Int>()
    return videos
        .asSequence()
        .filter { it.id.isNotBlank() && it.title.isNotBlank() }
        .sortedByDescending { it.timestamp }
        .distinctBy { it.id }
        .filter { video ->
            val key = video.channelId.ifBlank { video.channelName }
            if (key.isBlank()) return@filter true
            val count = channels.getOrDefault(key, 0)
            (count < 20).also { if (it) channels[key] = count + 1 }
        }.take(limit)
        .toList()
}

internal fun importedHistoryWeight(
    watchedAt: Long,
    now: Long,
): Double {
    if (watchedAt <= 0L) return 0.1
    val ageDays = (now - watchedAt).coerceAtLeast(0L) / 86_400_000.0
    return (0.1 + 0.9 * exp(-ageDays / 90.0)).coerceIn(0.1, 1.0)
}
