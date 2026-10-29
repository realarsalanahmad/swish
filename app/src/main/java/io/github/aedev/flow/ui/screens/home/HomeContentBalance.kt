package io.github.aedev.flow.ui.screens.home

import io.github.aedev.flow.data.model.Video

private val CONTENT_FAMILIES =
    mapOf(
        "stories" to listOf("horror", "ghost story", "scary", "creepypasta", "kahani", "stories", "storytelling"),
        "gaming" to listOf("gameplay", "gaming", "minecraft", "roblox", "fortnite", "pubg", "valorant"),
        "technology" to listOf("programming", "coding", "smartphone", "tech review", "android development", "javascript"),
        "food" to listOf("cooking", "recipe", "recipes", "food", "baking"),
        "fitness" to listOf("workout", "fitness", "bodybuilding", "exercise"),
        "news" to listOf("breaking news", "politics", "election", "samachar"),
        "comedy" to listOf("comedy", "standup", "stand-up", "prank"),
    )

internal fun homeContentFamily(video: Video): String? {
    if (isHomeMusic(video)) return "music"
    val title = video.title.lowercase()
    return CONTENT_FAMILIES.entries.firstOrNull { (_, words) -> words.any { containsFeedTerm(title, it) } }?.key
        ?: video.category?.lowercase()?.takeIf { it in setOf("gaming", "education", "sports", "science & technology") }
}
