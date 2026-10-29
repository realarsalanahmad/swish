package io.github.aedev.flow.data.account

import dagger.Lazy
import io.github.aedev.flow.data.engagement.FeedInvalidationBus
import io.github.aedev.flow.data.local.HomeFeedCacheRepository
import io.github.aedev.flow.data.recommendation.FlowNeuroEngine
import io.github.aedev.flow.data.recommendation.music.MusicBrainEngine
import io.github.aedev.flow.data.shorts.ShortsFeedRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

internal const val MAX_SWISH_PROFILE_BYTES = 2 * 1024 * 1024

internal fun validSwishProfile(profile: SwishProfile): Boolean =
    profile.videoBrain.toByteArray(Charsets.UTF_8).size + profile.musicBrain.toByteArray(Charsets.UTF_8).size <= MAX_SWISH_PROFILE_BYTES &&
        listOf(profile.videoBrain, profile.musicBrain).all { runCatching { Json.parseToJsonElement(it) is JsonObject }.getOrDefault(false) }

@Singleton
internal class SwishProfileManager
    @Inject
    constructor(
        private val videoEngine: Lazy<FlowNeuroEngine>,
        private val musicEngine: MusicBrainEngine,
        private val homeCache: HomeFeedCacheRepository,
        private val shorts: Lazy<ShortsFeedRepository>,
    ) {
        suspend fun export(): SwishProfile {
            val video = videoEngine.get()
            video.initialize()
            val videoBytes = ByteArrayOutputStream().also { video.exportBrainToStream(it) }
            val musicBytes = ByteArrayOutputStream().also { musicEngine.exportBrainToStream(it) }
            val profile = SwishProfile(videoBytes.toString("UTF-8"), musicBytes.toString("UTF-8"))
            if (!validSwishProfile(profile)) throw SwishAccountException(SwishAccountFailure.PROFILE_TOO_LARGE)
            return profile
        }

        suspend fun hasImportedHistory(): Boolean {
            val profile = export()
            val video = Json.parseToJsonElement(profile.videoBrain) as JsonObject
            return listOf("importedHistorySeeds", "importedHistoryIds").any { key ->
                (video[key] as? JsonArray)?.isNotEmpty() == true
            }
        }

        suspend fun restore(profile: SwishProfile) {
            if (!validSwishProfile(profile)) throw SwishAccountException(SwishAccountFailure.PROFILE_INVALID)
            val previous = export()
            try {
                if (!videoEngine.get().importBrainFromStream(profile.videoBrain.byteInputStream())) {
                    throw SwishAccountException(SwishAccountFailure.PROFILE_INVALID)
                }
                musicEngine.importBrainFromStream(profile.musicBrain.byteInputStream())
            } catch (error: Exception) {
                withContext(NonCancellable) {
                    videoEngine.get().importBrainFromStream(previous.videoBrain.byteInputStream())
                    musicEngine.importBrainFromStream(previous.musicBrain.byteInputStream())
                }
                if (error is CancellationException) throw error
                throw SwishAccountException(SwishAccountFailure.PROFILE_INVALID)
            }
            invalidate()
        }

        suspend fun reset(
            video: Boolean,
            music: Boolean,
        ) {
            videoEngine.get().initialize()
            if (video) videoEngine.get().resetBrain()
            if (music) musicEngine.resetBrain()
        }

        suspend fun invalidate() {
            homeCache.clearAll()
            shorts.get().clearCaches()
            FeedInvalidationBus.emit(FeedInvalidationBus.Event.PersonalizationChanged)
        }
    }
