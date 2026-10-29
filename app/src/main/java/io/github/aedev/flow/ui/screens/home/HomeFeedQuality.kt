package io.github.aedev.flow.ui.screens.home

import io.github.aedev.flow.data.model.Video
import io.github.aedev.flow.data.repository.looksLikeMusicVideo

private val UNSOLICITED_SUBJECTS =
    listOf(
        setOf(
            "horror",
            "scary stories",
            "scary story",
            "haunted",
            "ghost story",
            "ghost stories",
            "creepypasta",
            "bhoot",
            "darawani",
            "डरावनी",
            "भूत",
        ),
        setOf("news", "breaking news", "politics", "political", "election", "war", "samachar", "खबर", "समाचार"),
        setOf(
            "religion",
            "religious",
            "spiritual",
            "spirituality",
            "sermon",
            "prayer",
            "devotional",
            "bhajan",
            "aarti",
            "satsang",
            "धर्म",
            "भजन",
            "आरती",
        ),
    )
private val CLICKBAIT_PHRASES =
    listOf(
        "you won't believe",
        "you won’t believe",
        "shocking truth",
        "watch before it gets deleted",
        "miracle cure",
        "100% cure",
        "ai generated story",
        "ai-generated story",
        "ai generated stories",
        "ai slop",
        "brainrot",
        "brain rot",
        "ai horror story",
        "ai horror stories",
        "ai generated horror",
        "ai-generated horror",
    )
private val BULK_FORMATS = listOf("hours of", "hour of", "full movie", "full episode", "marathon", "all parts")
private val AUDIO_FORMATS =
    listOf("official audio", "audio only", "audio-only", "lyrics", "lyric video", "lyrical", "visualizer", "visualiser")
private val MUSIC_VIDEO_FORMATS = listOf("music video", "official video", "official mv", "video song")
private val MUSIC_FORMATS = listOf("song", "music", "remix", "karaoke", "instrumental", "lofi", "lo-fi")

internal fun isHomeMusic(video: Video): Boolean {
    val title = video.title.lowercase()
    if (listOf("tutorial", "lesson", "review", "how to", "theory").any { containsFeedTerm(title, it) } &&
        (MUSIC_VIDEO_FORMATS + AUDIO_FORMATS).none { containsFeedTerm(title, it) }
    ) {
        return false
    }
    return video.isMusic || video.category.equals("Music", ignoreCase = true) ||
        looksLikeMusicVideo(video.title, video.channelName) ||
        (MUSIC_FORMATS + MUSIC_VIDEO_FORMATS + AUDIO_FORMATS).any { containsFeedTerm(title, it) }
}

internal fun List<Video>.limitHomeMusic(budget: Int): List<Video> {
    var count = 0
    return filter { !isHomeMusic(it) || count++ < budget }
}

internal fun List<Video>.limitFollowedShare(
    followed: Set<String>,
    budget: Int,
): List<Video> {
    var count = 0
    return filter { it.channelId !in followed || count++ < budget }
}

internal fun containsFeedTerm(
    text: String,
    term: String,
): Boolean {
    val needle = term.lowercase().trim()
    if (needle.length < 3) return false
    var start = text.indexOf(needle)
    while (start >= 0) {
        val end = start + needle.length
        if ((start == 0 || !text[start - 1].isLetterOrDigit()) && (end == text.length || !text[end].isLetterOrDigit())) return true
        start = text.indexOf(needle, start + 1)
    }
    return false
}

internal fun rejectsHomeCandidate(
    video: Video,
    taste: FeedTasteProfile,
): Boolean {
    val title = video.title.lowercase()
    val musicVideo = MUSIC_VIDEO_FORMATS.any { containsFeedTerm(title, it) }
    if (!musicVideo && AUDIO_FORMATS.any { containsFeedTerm(title, it) }) return true
    if (isHomeMusic(video) && !musicVideo && !video.channelName.endsWith("VEVO", ignoreCase = true)) {
        return true
    }
    if (CLICKBAIT_PHRASES.any { title.contains(it) }) return true
    if (UNSOLICITED_SUBJECTS.any { subject ->
            subject.any { containsFeedTerm(title, it) } &&
                taste.affinityTopics.none { interest -> subject.any { containsFeedTerm(interest, it) } }
        }
    ) {
        return true
    }
    return taste.comfortDurationSec > 0 && video.duration > taste.comfortDurationSec &&
        BULK_FORMATS.any { title.contains(it) } && taste.affinityTopics.none { containsFeedTerm(title, it) }
}
