package io.github.aedev.flow.data.account

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

class SwishAccountValidationTest {
    @Test fun `account service requires HTTPS without URL credentials`() {
        assertThat(swishApiBaseUrl("https://swish-dusky.vercel.app/")).isEqualTo("https://swish-dusky.vercel.app")
        listOf("http://example.com", "https://name:password@example.com", "https://example.com?token=x", "https://example.com#x", "garbage")
            .forEach { assertThat(swishApiBaseUrl(it)).isNull() }
    }

    @Test fun `expired or malformed sessions cannot be restored`() {
        val now = Instant.parse("2026-10-29T00:00:00Z")
        val session = SwishSession(SwishUser("id", "user@example.com", "User"), "a".repeat(43), "2026-11-01T00:00:00Z")
        assertThat(validSwishSession(session, now)).isTrue()
        assertThat(validSwishSession(session.copy(token = "wrong"), now)).isFalse()
        assertThat(validSwishSession(session.copy(expiresAt = now.toString()), now)).isFalse()
    }

    @Test fun `profile rejects arrays malformed JSON and oversized history`() {
        assertThat(validSwishProfile(SwishProfile("{}", "{}"))).isTrue()
        assertThat(validSwishProfile(SwishProfile("[]", "{}"))).isFalse()
        assertThat(validSwishProfile(SwishProfile("invalid", "{}"))).isFalse()
        assertThat(validSwishProfile(SwishProfile("{\"large\":\"" + "x".repeat(MAX_SWISH_PROFILE_BYTES) + "\"}", "{}"))).isFalse()
    }
}
