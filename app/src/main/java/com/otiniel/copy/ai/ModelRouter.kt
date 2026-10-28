package com.otiniel.copy.ai

import com.otiniel.copy.data.ChatMessage
import com.otiniel.copy.data.ConnectionSettings

/** Provider boundary: local inference and more providers can be added without coupling the UI. */
class ModelRouter(
    private val remoteProvider: OpenAiCompatibleProvider = OpenAiCompatibleProvider()
) {
    suspend fun reply(settings: ConnectionSettings, transcript: List<ChatMessage>): String {
        if (settings.endpoint.isBlank() || settings.model.isBlank()) {
            return "O núcleo local do COPY está pronto para continuar a implementação, mas nenhum modelo de IA foi configurado. Abra Configurações e informe um endpoint HTTPS compatível, o nome do modelo e uma chave se o provedor exigir. Nenhuma mensagem foi enviada para a internet nesta resposta."
        }
        return remoteProvider.complete(settings, transcript)
    }

    suspend fun testConnection(settings: ConnectionSettings): String {
        require(settings.endpoint.isNotBlank()) { "Informe o endpoint HTTPS." }
        require(settings.model.isNotBlank()) { "Informe o identificador do modelo." }
        return remoteProvider.testConnection(settings)
    }
}
