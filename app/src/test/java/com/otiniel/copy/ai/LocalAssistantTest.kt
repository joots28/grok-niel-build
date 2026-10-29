package com.otiniel.copy.ai

import com.otiniel.copy.data.ChatMessage
import com.otiniel.copy.data.MemoryEntry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAssistantTest {
    @Test
    fun greetingExplainsOfflineLimitHonestly() {
        val reply = LocalAssistant.offlineReply("Oi!")
        assertTrue(reply.contains("modo local"))
        assertTrue(reply.contains("respostas livres com IA"))
    }

    @Test
    fun helpCommandWorksWithoutProvider() {
        val reply = LocalAssistant.answerCommand("/ajuda", emptyList(), emptyList())
        assertNotNull(reply)
        assertTrue(reply!!.contains("/memorias"))
    }

    @Test
    fun memoryCommandUsesLocalNotesWithoutNetwork() {
        val notes = listOf(MemoryEntry(1, "Projeto", "COPY usa Kotlin", "manual", 1L))
        val reply = LocalAssistant.answerCommand("/memorias", emptyList(), notes)
        assertNotNull(reply)
        assertTrue(reply!!.contains("COPY usa Kotlin"))
    }

    @Test
    fun localCommandsDoNotNeedRemoteProvider() {
        val transcript = listOf(ChatMessage(1, "user", "/data", 1L))
        val result = LocalAssistant.answerCommand("/data", transcript, emptyList())
        assertNotNull(result)
        assertTrue(result!!.contains("Data e hora"))
        assertFalse(result.orEmpty().contains("HTTP"))
    }
}
