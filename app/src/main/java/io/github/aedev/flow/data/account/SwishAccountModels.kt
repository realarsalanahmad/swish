package io.github.aedev.flow.data.account

import kotlinx.serialization.Serializable

@Serializable
internal data class SwishUser(
    val id: String,
    val email: String,
    val name: String,
)

@Serializable
internal data class SwishSession(
    val user: SwishUser,
    val token: String,
    val expiresAt: String,
)

@Serializable
internal data class SwishProfile(
    val videoBrain: String,
    val musicBrain: String,
    val savedAt: String? = null,
)

@Serializable
internal data class SwishPendingImport(
    val profile: SwishProfile,
    val ownerId: String? = null,
)

@Serializable
internal data class SwishLoginRequest(
    val email: String,
    val password: String,
)

@Serializable
internal data class SwishRegistrationRequest(
    val name: String,
    val email: String,
    val password: String,
)

@Serializable
internal data class SwishApiError(
    val code: String = "service_unavailable",
)

@Serializable
internal data class SwishProfileSaved(
    val savedAt: String,
)

internal enum class SwishAccountFailure {
    INVALID_INPUT,
    INVALID_CREDENTIALS,
    EMAIL_IN_USE,
    SESSION_EXPIRED,
    PROFILE_MISSING,
    PROFILE_TOO_LARGE,
    PROFILE_INVALID,
    RATE_LIMITED,
    UNAVAILABLE,
}

internal class SwishAccountException(
    val failure: SwishAccountFailure,
) : Exception()

internal enum class SwishAccountNotice { PROFILE_SAVED, PROFILE_RESTORED, PROFILE_RESET, SIGNED_OUT }

internal data class SwishAccountState(
    val initialized: Boolean = false,
    val user: SwishUser? = null,
    val busy: Boolean = false,
    val failure: SwishAccountFailure? = null,
    val notice: SwishAccountNotice? = null,
)
