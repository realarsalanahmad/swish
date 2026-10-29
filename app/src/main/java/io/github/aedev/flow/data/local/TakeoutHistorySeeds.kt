package io.github.aedev.flow.data.local

import java.util.TreeSet

internal class TakeoutHistorySeeds(
    limitPerKind: Int = 800,
) {
    private val videos = RecentHistorySeeds(limitPerKind)
    private val music = RecentHistorySeeds(limitPerKind)
    val values: Collection<VideoHistoryEntry> get() = videos.values + music.values

    fun offer(entry: VideoHistoryEntry) {
        if (entry.isMusic) music.offer(entry) else videos.offer(entry)
    }
}

private class RecentHistorySeeds(
    private val limit: Int,
) {
    private val byId = HashMap<String, VideoHistoryEntry>()
    private val ordered = TreeSet(compareBy<VideoHistoryEntry> { it.timestamp }.thenBy { it.videoId })
    val values: List<VideoHistoryEntry> get() = ordered.descendingSet().toList()

    init {
        require(limit >= 0)
    }

    fun offer(entry: VideoHistoryEntry) {
        if (limit == 0 || entry.videoId.isBlank() || entry.title.isBlank()) return
        val previous = byId[entry.videoId]
        if (previous != null && previous.timestamp >= entry.timestamp) return
        if (previous != null) ordered.remove(previous)
        ordered.add(entry)
        byId[entry.videoId] = entry
        if (ordered.size > limit) {
            val oldest = ordered.pollFirst()
            byId.remove(oldest.videoId)
        }
    }
}
