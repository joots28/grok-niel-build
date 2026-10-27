package com.otiniel.copy.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class EndpointUrlTest {
    @Test
    fun rootUrlGetsChatCompletionsPath() {
        assertEquals("https://example.com/v1/chat/completions", EndpointUrl.chatCompletions("https://example.com"))
    }

    @Test
    fun versionedUrlGetsChatCompletionsPath() {
        assertEquals("https://example.com/v1/chat/completions", EndpointUrl.chatCompletions("https://example.com/v1/"))
    }

    @Test
    fun fullEndpointIsPreserved() {
        assertEquals(
            "https://example.com/custom/chat/completions",
            EndpointUrl.chatCompletions("https://example.com/custom/chat/completions")
        )
    }

    @Test
    fun customPrefixGetsVersionedPath() {
        assertEquals("https://example.com/gateway/v1/chat/completions", EndpointUrl.chatCompletions("https://example.com/gateway"))
    }

    @Test
    fun rejectsUnencryptedHttp() {
        assertThrows(IllegalArgumentException::class.java) {
            EndpointUrl.chatCompletions("http://example.com/v1")
        }
    }

    @Test
    fun rejectsEmbeddedCredentials() {
        assertThrows(IllegalArgumentException::class.java) {
            EndpointUrl.chatCompletions("https://user:secret@example.com/v1")
        }
    }

    @Test
    fun rejectsQueryParameters() {
        assertThrows(IllegalArgumentException::class.java) {
            EndpointUrl.chatCompletions("https://example.com/v1?key=secret")
        }
    }
}
