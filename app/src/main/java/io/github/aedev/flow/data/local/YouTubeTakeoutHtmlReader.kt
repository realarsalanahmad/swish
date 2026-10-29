package io.github.aedev.flow.data.local

import java.io.Reader

// Every Takeout activity entry sits in this cell, whatever language the archive is in.
private const val TAKEOUT_ACTIVITY_MARKUP = "content-cell"
private const val READ_SIZE = 65_536
private const val OVERLAP = 2_048
private const val BATCH_SIZE = 500
private const val CHANNEL_LINK_REACH = 2_000

private val watchLinkPattern =
    Regex("""href="https://(www|music)\.youtube\.com/(?:watch\?v=|shorts/)([\w-]{11})"[^>]*?>([^<]+)</a>""", RegexOption.IGNORE_CASE)
private val channelLinkPattern =
    Regex("""href="https://(?:www|music)\.youtube\.com/channel/([^"&\s]+)"[^>]*?>([^<]+)</a>""", RegexOption.IGNORE_CASE)
private val searchLinkPattern =
    Regex("""href="(https://(?:www|music)\.youtube\.com/(?:results|search)\?[^"]+)"""", RegexOption.IGNORE_CASE)

private val outerCellPattern = Regex("""<div[^>]*class=["'][^"']*\bouter-cell\b[^"']*["'][^>]*>""", RegexOption.IGNORE_CASE)
private val contentCellPattern = Regex("""<div[^>]*class=["'][^"']*\bcontent-cell\b[^"']*["'][^>]*>""", RegexOption.IGNORE_CASE)

/** A video watched, as a Takeout activity file records it. */
internal data class TakeoutWatch(
    val videoId: String,
    val title: String,
    val channelName: String,
    val channelId: String,
    val watchedAt: Long,
    val isMusic: Boolean,
    val isShort: Boolean = false,
)

/** What a Takeout activity HTML file held. [watches] counts what was handed to the batch callback. */
internal data class TakeoutHtmlActivity(
    val watches: Int,
    val searches: List<TakeoutSearch>,
    val adVideoIds: Set<String> = emptySet(),
)

/**
 * Streams a Takeout activity HTML file, handing the videos watched on YouTube and YouTube Music to
 * [onWatches] in batches and keeping the searches. Takeout names the watch and search history files
 * in the account's language, so both are read from every file. With
 * [requireActivityMarkup], a file that never shows the My Activity layout gives nothing, so other
 * HTML in the archive is ignored. Recognized dates retain their age; unknown localized dates stay unknown.
 */
internal suspend fun readTakeoutHtmlActivity(
    reader: Reader,
    now: Long,
    requireActivityMarkup: Boolean,
    onWatches: suspend (List<TakeoutWatch>) -> Unit,
): TakeoutHtmlActivity {
    var markupSeen = !requireActivityMarkup
    var watched = 0
    val batch = mutableListOf<TakeoutWatch>()
    val searches = mutableListOf<TakeoutSearch>()
    val adVideoIds = mutableSetOf<String>()
    val buffer = CharArray(READ_SIZE)
    val tail = StringBuilder(OVERLAP)

    suspend fun flush() {
        if (batch.isEmpty()) return
        onWatches(batch.toList())
        batch.clear()
    }

    while (true) {
        val count = reader.read(buffer)
        val finished = count == -1
        val pending = tail.toString() + if (finished) "" else String(buffer, 0, count)
        val cells = outerCellPattern.findAll(pending).toList().ifEmpty { contentCellPattern.findAll(pending).toList() }
        val end = if (finished) pending.length else cells.lastOrNull()?.range?.first ?: (pending.length - OVERLAP).coerceAtLeast(0)
        val window = pending.substring(0, end)
        if (!markupSeen) markupSeen = window.contains(TAKEOUT_ACTIVITY_MARKUP)

        val channels = channelLinkPattern.findAll(window).toList()
        val activityStarts =
            outerCellPattern
                .findAll(window)
                .toList()
                .ifEmpty {
                    contentCellPattern.findAll(window).toList()
                }.map { it.range.first }
        var channelIndex = 0
        for (match in watchLinkPattern.findAll(window)) {
            val cellIndex = activityStarts.binarySearch(match.range.first).let { if (it < 0) -it - 2 else it }
            val cellStart = activityStarts.getOrNull(cellIndex) ?: 0
            val cellEnd = activityStarts.getOrNull(cellIndex + 1) ?: window.length
            if (isTakeoutAdActivity(window.substring(cellStart, cellEnd).unescapedHtml())) {
                adVideoIds += match.groupValues[2]
                continue
            }
            while (channelIndex < channels.size && channels[channelIndex].range.first <= match.range.first) channelIndex++
            val channel =
                channels.getOrNull(channelIndex)?.takeIf {
                    it.range.first < cellEnd &&
                        it.range.first - match.range.last < CHANNEL_LINK_REACH
                }
            batch +=
                TakeoutWatch(
                    videoId = match.groupValues[2].trim(),
                    title = match.groupValues[3].trim().unescapedHtml(),
                    channelName =
                        channel
                            ?.groupValues
                            ?.get(2)
                            ?.trim()
                            ?.unescapedHtml()
                            .orEmpty(),
                    channelId =
                        channel
                            ?.groupValues
                            ?.get(1)
                            ?.trim()
                            .orEmpty(),
                    watchedAt =
                        takeoutHtmlWatchedAt(
                            window
                                .substring(match.range.last + 1, minOf(window.length, match.range.last + CHANNEL_LINK_REACH))
                                .substringBefore("</div>"),
                        ),
                    isMusic = match.groupValues[1].equals("music", ignoreCase = true),
                    isShort = match.value.contains("/shorts/", ignoreCase = true),
                )
        }

        for (match in searchLinkPattern.findAll(window)) {
            val (query, isMusic) = takeoutSearchOf(match.groupValues[1].unescapedHtml()) ?: continue
            searches += TakeoutSearch(query, now - searches.size, isMusic)
        }

        tail.clear()
        tail.append(pending, end, pending.length)
        if (markupSeen && batch.size >= BATCH_SIZE) {
            watched += batch.size
            flush()
        }
        if (finished) break
    }
    if (!markupSeen) return TakeoutHtmlActivity(0, emptyList())
    watched += batch.size
    flush()
    return TakeoutHtmlActivity(watched, searches, adVideoIds)
}

internal fun String.unescapedHtml(): String =
    replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&#x27;", "'")
        .replace("&amp;", "&")
