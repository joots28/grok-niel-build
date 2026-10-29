package com.otiniel.copy.ai

import com.otiniel.copy.data.ChatMessage
import com.otiniel.copy.data.ConnectionSettings
import com.otiniel.copy.data.MemoryEntry

/**
 * Routes private commands locally and only sends opted-in memory to the remote model.
 */
class ModelRouter(
    private val remoteProvider: OpenAiCompatibleProvider = OpenAiCompatibleProvider()
) {
    suspend fun reply(
        settings: ConnectionSettings,
        transcript: List<ChatMessage>,
        localMemories: List<MemoryEntry> = emptyList()
    ): String {
        val latestUserMessage = transcript.lastOrNull { it.role == "user" }?.content.orEmpty()
        LocalAssistant.answerCommand(latestUserMessage, transcript, localMemories)?.let { return it }

        if (settings.endpoint.isBlank() || settings.model.isBlank()) {
            return LocalAssistant.offlineReply(latestUserMessage)
        }

        val memoriesToShare = if (settings.includeMemories) localMemories else emptyList()
        return remoteProvider.complete(settings, transcript, memoriesToShare)
    }

    suspend fun testConnection(settings: ConnectionSettings): String {
        require(settings.endpoint.isNotBlank()) { "Informe o endpoint HTTPS." }
        require(settings.model.isNotBlank()) { "Informe o identificador do modelo." }
        return remoteProvider.testConnection(settings)
    }
}
