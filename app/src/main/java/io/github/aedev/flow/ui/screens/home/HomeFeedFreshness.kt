package io.github.aedev.flow.ui.screens.home

import io.github.aedev.flow.data.model.Video
import kotlin.math.pow

private const val DAY_MS = 86_400_000L

internal fun channelFreshnessScores(
    videos: List<Video>,
    now: Long,
): Map<String, Double> {
    val dated = videos.filter { it.timestamp > 0L && it.timestamp <= now && (it.timestampIsExact || it.uploadDate.isNotBlank()) }
    return dated
        .groupBy { it.channelId.ifBlank { it.id } }
        .values
        .flatMap { uploads ->
            val dates = uploads.map { it.timestamp }.distinct().sortedDescending()
            val gaps = dates.zipWithNext { latest, older -> (latest - older).toDouble() / DAY_MS }.filter { it > 0 }.sorted()
            val cadence = gaps.takeIf { it.size >= 2 }?.let { it[it.size / 2] }
            val halfLifeDays =
                when {
                    cadence == null -> 21.0
                    cadence <= 2.0 -> 3.0
                    cadence <= 10.0 -> 14.0
                    else -> 45.0
                }
            uploads.map { video ->
                val age = (now - video.timestamp).toDouble() / DAY_MS
                video.id to (0.32 * 0.5.pow(age / halfLifeDays))
            }
        }.toMap()
}

internal fun preferRecentUploads(
    ranked: List<Video>,
    now: Long = System.currentTimeMillis(),
): List<Video> {
    if (ranked.size < 2) return ranked
    val freshness = channelFreshnessScores(ranked, now)
    return ranked
        .withIndex()
        .sortedByDescending { (index, video) ->
            1.0 - index.toDouble() / ranked.size + freshness.getOrDefault(video.id, 0.0)
        }.map { it.value }
}
