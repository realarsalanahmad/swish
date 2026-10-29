package io.github.aedev.flow.data.local

private val AD_ACTIVITY_MARKERS =
    listOf("from google ads", "from advertising", "watched an ad", "viewed an ad", "watched ad for", "ad was shown")

internal fun isTakeoutAdActivity(text: String): Boolean {
    val normalized = text.lowercase()
    return AD_ACTIVITY_MARKERS.any(normalized::contains)
}
