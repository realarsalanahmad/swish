package io.github.aedev.flow.data.account

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.aedev.flow.data.local.KeystoreSecretBox
import io.github.aedev.flow.data.local.safePreferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.swishAccountDataStore by safePreferencesDataStore(name = "swish_account")

@Singleton
internal class SwishAccountStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val sessionKey = stringPreferencesKey("sealed_session")
        private val pendingImportKey = stringPreferencesKey("sealed_pending_import")
        private val importTrackingKey = booleanPreferencesKey("import_tracking_initialized")
        private val json = Json { ignoreUnknownKeys = true }

        suspend fun load(): SwishSession? =
            withContext(Dispatchers.IO) {
                val stored = KeystoreSecretBox.open(context.swishAccountDataStore.data.first()[sessionKey])
                if (stored.isBlank()) return@withContext null
                runCatching { json.decodeFromString<SwishSession>(stored) }
                    .getOrNull()
                    ?.takeIf { validSwishSession(it) }
                    .also { if (it == null) clear() }
            }

        suspend fun save(session: SwishSession) =
            withContext(Dispatchers.IO) {
                val sealed = KeystoreSecretBox.seal(json.encodeToString(session))
                context.swishAccountDataStore.edit { it[sessionKey] = sealed }
                Unit
            }

        suspend fun clear() {
            context.swishAccountDataStore.edit { it.remove(sessionKey) }
        }

        suspend fun importTrackingInitialized(): Boolean = context.swishAccountDataStore.data.first()[importTrackingKey] == true

        suspend fun pendingImport(): SwishPendingImport? =
            withContext(Dispatchers.IO) {
                val sealed = context.swishAccountDataStore.data.first()[pendingImportKey] ?: return@withContext null
                val stored = KeystoreSecretBox.open(sealed)
                if (stored.isBlank()) throw SwishAccountException(SwishAccountFailure.PROFILE_INVALID)
                json.decodeFromString<SwishPendingImport>(stored)
            }

        suspend fun savePendingImport(pending: SwishPendingImport) =
            withContext(Dispatchers.IO) {
                val sealed = KeystoreSecretBox.seal(json.encodeToString(pending))
                context.swishAccountDataStore.edit {
                    it[pendingImportKey] = sealed
                    it[importTrackingKey] = true
                }
                Unit
            }

        suspend fun finishImportTracking() {
            context.swishAccountDataStore.edit {
                it.remove(pendingImportKey)
                it[importTrackingKey] = true
            }
        }
    }
