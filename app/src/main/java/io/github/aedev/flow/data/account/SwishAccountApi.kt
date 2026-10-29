package io.github.aedev.flow.data.account

import io.github.aedev.flow.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

internal fun swishApiBaseUrl(value: String): String? =
    value
        .trim()
        .toHttpUrlOrNull()
        ?.takeIf { it.isHttps && it.username.isEmpty() && it.password.isEmpty() && it.query == null && it.fragment == null }
        ?.toString()
        ?.trimEnd('/')

internal fun validSwishSession(
    session: SwishSession,
    now: Instant = Instant.now(),
): Boolean =
    session.user.id.isNotBlank() &&
        session.user.email.isNotBlank() &&
        session.token.matches(Regex("[A-Za-z0-9_-]{43}")) &&
        runCatching { Instant.parse(session.expiresAt).isAfter(now) }.getOrDefault(false)

@Singleton
internal class SwishAccountApi
    @Inject
    constructor(
        okHttpClient: OkHttpClient,
    ) {
        private val baseUrl = swishApiBaseUrl(BuildConfig.SWISH_API_BASE_URL)
        val configured: Boolean get() = baseUrl != null
        private val client by lazy {
            HttpClient(OkHttp) {
                engine {
                    preconfigured =
                        okHttpClient
                            .newBuilder()
                            .followRedirects(false)
                            .followSslRedirects(false)
                            .build()
                }
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                install(HttpTimeout) {
                    connectTimeoutMillis = 10_000
                    socketTimeoutMillis = 20_000
                    requestTimeoutMillis = 35_000
                }
                expectSuccess = false
            }
        }

        suspend fun login(
            email: String,
            password: String,
        ): SwishSession =
            request("/v1/auth/login", HttpMethod.Post, payload = Json.encodeToJsonElement(SwishLoginRequest(email, password))).body()

        suspend fun register(
            name: String,
            email: String,
            password: String,
        ): SwishSession =
            request(
                "/v1/auth/register",
                HttpMethod.Post,
                payload = Json.encodeToJsonElement(SwishRegistrationRequest(name, email, password)),
            ).body()

        suspend fun logout(token: String) {
            request("/v1/auth/logout", HttpMethod.Post, token)
        }

        suspend fun saveProfile(
            token: String,
            profile: SwishProfile,
        ): SwishProfileSaved =
            request(
                "/v1/profile",
                HttpMethod.Put,
                token,
                Json.encodeToJsonElement(profile.copy(savedAt = null)),
            ).body()

        suspend fun profile(token: String): SwishProfile = request("/v1/profile", HttpMethod.Get, token).body()

        private suspend fun request(
            path: String,
            method: HttpMethod,
            token: String? = null,
            payload: JsonElement? = null,
        ): HttpResponse {
            val endpoint = baseUrl ?: throw SwishAccountException(SwishAccountFailure.UNAVAILABLE)
            val response =
                client.request(endpoint + path) {
                    this.method = method
                    contentType(ContentType.Application.Json)
                    token?.let { bearerAuth(it) }
                    payload?.let { setBody(it) }
                }
            if (response.status.value !in 200..299) {
                val code = runCatching { response.body<SwishApiError>().code }.getOrDefault("service_unavailable")
                val failure =
                    when (code) {
                        "invalid_input" -> SwishAccountFailure.INVALID_INPUT
                        "invalid_credentials" -> SwishAccountFailure.INVALID_CREDENTIALS
                        "email_in_use" -> SwishAccountFailure.EMAIL_IN_USE
                        "session_expired" -> SwishAccountFailure.SESSION_EXPIRED
                        "profile_missing" -> SwishAccountFailure.PROFILE_MISSING
                        "profile_too_large" -> SwishAccountFailure.PROFILE_TOO_LARGE
                        "invalid_profile" -> SwishAccountFailure.PROFILE_INVALID
                        "rate_limited" -> SwishAccountFailure.RATE_LIMITED
                        else -> SwishAccountFailure.UNAVAILABLE
                    }
                throw SwishAccountException(failure)
            }
            return response
        }
    }
