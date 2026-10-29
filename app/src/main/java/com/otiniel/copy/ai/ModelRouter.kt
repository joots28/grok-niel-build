package com.otiniel.copy.ai

import com.otiniel.copy.data.ChatMessage
import com.otiniel.copy.data.ConnectionSettings
import com.otiniel.copy.data.MemoryEntry

/**
 * Routes local commands first, then the bundled on-device model, and only uses
 * a remote provider if no local model has been prepared on this device.
 */
class ModelRouter(
    private val remoteProvider: OpenAiCompatibleProvider = OpenAiCompatibleProvider(),
    private val localModelEngine: LocalModelEngine? = null
) {
    suspend fun reply(
        settings: ConnectionSettings,
        transcript: List<ChatMessage>,
        localMemories: List<MemoryEntry> = emptyList()
    ): String {
        val latestUserMessage = transcript.lastOrNull { it.role == "user" }?.content.orEmpty()
        LocalAssistant.answerCommand(latestUserMessage, transcript, localMemories)?.let { return it }

        // Once prepared, local inference has priority. An inference error is returned
        // to the user; it must not silently send the same private conversation online.
        if (localModelEngine?.isPrepared() == true) {
            return localModelEngine.reply(transcript, localMemories)
        }

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
