package io.github.aedev.flow.data.account

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SwishAccountRepository
    @Inject
    internal constructor(
        private val api: SwishAccountApi,
        private val store: SwishAccountStore,
        private val profiles: SwishProfileManager,
    ) {
        private val mutableState = MutableStateFlow(SwishAccountState())
        internal val state = mutableState.asStateFlow()
        val configured: Boolean get() = api.configured
        private val initialization = Mutex()
        private val work = Mutex()
        private var session: SwishSession? = null

        suspend fun initialize() =
            initialization.withLock {
                if (state.value.initialized) return@withLock
                try {
                    session = store.load()
                    mutableState.value = SwishAccountState(initialized = true, user = session?.user)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    mutableState.value = SwishAccountState(initialized = true, failure = SwishAccountFailure.UNAVAILABLE)
                }
            }

        suspend fun login(
            email: String,
            password: String,
        ) = run {
            prepareExistingImport()
            acceptSession(api.login(email.trim().lowercase(Locale.ROOT), password))
            restoreOrSaveImport()
        }

        private suspend fun prepareExistingImport() {
            if (!store.importTrackingInitialized() && profiles.hasImportedHistory()) {
                store.savePendingImport(SwishPendingImport(profiles.export(), session?.user?.id))
            }
        }

        private suspend fun restoreOrSaveImport() {
            val current = checkNotNull(session)
            val pending = store.pendingImport()
            if (pending != null && (pending.ownerId == null || pending.ownerId == current.user.id)) {
                if (pending.ownerId == null) store.savePendingImport(pending.copy(ownerId = current.user.id))
                api.saveProfile(current.token, pending.profile)
                profiles.restore(pending.profile)
                store.finishImportTracking()
                mutableState.update { it.copy(notice = SwishAccountNotice.PROFILE_SAVED) }
                return
            }
            try {
                profiles.restore(api.profile(current.token))
                mutableState.update { it.copy(notice = SwishAccountNotice.PROFILE_RESTORED) }
            } catch (error: SwishAccountException) {
                if (error.failure != SwishAccountFailure.PROFILE_MISSING) throw error
            }
        }

        suspend fun register(
            name: String,
            email: String,
            password: String,
        ) = run {
            prepareExistingImport()
            acceptSession(api.register(name.trim(), email.trim().lowercase(Locale.ROOT), password))
            if (store.pendingImport() != null) restoreOrSaveImport()
        }

        private suspend fun acceptSession(candidate: SwishSession) {
            if (!validSwishSession(candidate)) throw SwishAccountException(SwishAccountFailure.UNAVAILABLE)
            store.save(candidate)
            session = candidate
            mutableState.update { it.copy(user = candidate.user) }
        }

        suspend fun logout() =
            run {
                val current = session ?: return@run
                prepareExistingImport()
                api.logout(current.token)
                store.clear()
                session = null
                mutableState.update { it.copy(user = null, notice = SwishAccountNotice.SIGNED_OUT) }
            }

        suspend fun saveProfile(expectedUserId: String? = state.value.user?.id): Boolean? {
            initialize()
            if (expectedUserId == null || session?.user?.id != expectedUserId) return null
            return run {
                val current = session ?: throw SwishAccountException(SwishAccountFailure.SESSION_EXPIRED)
                if (current.user.id != expectedUserId) throw SwishAccountException(SwishAccountFailure.UNAVAILABLE)
                api.saveProfile(current.token, profiles.export())
                val pending = store.pendingImport()
                if (pending == null || pending.ownerId == null || pending.ownerId == current.user.id) store.finishImportTracking()
                mutableState.update { it.copy(notice = SwishAccountNotice.PROFILE_SAVED) }
            }
        }

        suspend fun takeoutImported(expectedUserId: String?): Boolean? {
            initialize()
            if (expectedUserId != null && session?.user?.id != expectedUserId) return null
            return run(waitForWork = true) {
                if (expectedUserId != null &&
                    session?.user?.id != expectedUserId
                ) {
                    throw SwishAccountException(SwishAccountFailure.UNAVAILABLE)
                }
                val profile = profiles.export()
                val current = session
                store.savePendingImport(SwishPendingImport(profile, current?.user?.id))
                if (current != null) {
                    api.saveProfile(current.token, profile)
                    store.finishImportTracking()
                    mutableState.update { it.copy(notice = SwishAccountNotice.PROFILE_SAVED) }
                }
            }
        }

        suspend fun restoreProfile() =
            run {
                val current = session ?: throw SwishAccountException(SwishAccountFailure.SESSION_EXPIRED)
                profiles.restore(api.profile(current.token))
                mutableState.update { it.copy(notice = SwishAccountNotice.PROFILE_RESTORED) }
            }

        suspend fun personalizationChanged() = profiles.invalidate()

        suspend fun resetProfile(
            video: Boolean = true,
            music: Boolean = true,
        ) = run {
            val previous = profiles.export()
            try {
                profiles.reset(video, music)
                session?.let { api.saveProfile(it.token, profiles.export()) }
                store.finishImportTracking()
            } catch (error: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) { profiles.restore(previous) }
                throw error
            }
            profiles.invalidate()
            mutableState.update { it.copy(notice = SwishAccountNotice.PROFILE_RESET) }
        }

        private suspend fun run(
            waitForWork: Boolean = false,
            action: suspend () -> Unit,
        ): Boolean {
            initialize()
            if (waitForWork) {
                work.lock()
            } else if (!work.tryLock()) {
                return false
            }
            mutableState.update { it.copy(busy = true, failure = null, notice = null) }
            return try {
                action()
                true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                val failure = (error as? SwishAccountException)?.failure ?: SwishAccountFailure.UNAVAILABLE
                if (failure == SwishAccountFailure.SESSION_EXPIRED) {
                    store.clear()
                    session = null
                }
                mutableState.update { it.copy(failure = failure, user = session?.user) }
                false
            } finally {
                mutableState.update { it.copy(busy = false) }
                work.unlock()
            }
        }
    }
