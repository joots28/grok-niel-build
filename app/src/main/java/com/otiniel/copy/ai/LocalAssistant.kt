package com.otiniel.copy.ai

import com.otiniel.copy.data.ChatMessage
import com.otiniel.copy.data.MemoryEntry
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Deterministic commands that run on-device with no model, key, or network.
 * This is intentionally not presented as generative AI.
 */
object LocalAssistant {
    fun answerCommand(
        rawText: String,
        transcript: List<ChatMessage>,
        memories: List<MemoryEntry>
    ): String? {
        return when (rawText.trim().lowercase(Locale.ROOT)) {
            "/ajuda" -> """
                Comandos locais do COPY

                /ajuda — mostra esta ajuda.
                /data — mostra a data e hora do aparelho.
                /memorias — consulta memórias guardadas neste aparelho.
                /resumo — lista trechos recentes da conversa.

                Para respostas generativas, configure um modelo em Ajustes. Os comandos acima não enviam dados à internet.
            """.trimIndent()
            "/data" -> "Data e hora do aparelho: " + ZonedDateTime.now().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm:ss z", Locale("pt", "BR"))
            )
            "/memorias" -> {
                if (memories.isEmpty()) {
                    "Você ainda não salvou nenhuma memória. Abra a aba Memória para adicionar uma."
                } else {
                    "Memórias salvas localmente (${memories.size}):\n\n" +
                        memories.take(20).joinToString("\n\n") { memory ->
                            "• ${memory.title}\n${memory.content.take(350)}"
                        } +
                        if (memories.size > 20) "\n\nMostrando as 20 primeiras." else ""
                }
            }
            "/resumo" -> {
                val recent = transcript.filter { it.role == "user" || it.role == "assistant" }.takeLast(8)
                if (recent.isEmpty()) "Ainda não há mensagens suficientes para resumir."
                else "Trechos recentes da conversa (modo local, sem síntese por IA):\n\n" +
                    recent.joinToString("\n\n") { message ->
                        val speaker = if (message.role == "user") "Você" else "COPY"
                        "$speaker: " + message.content.replace("\n", " ").take(220)
                    }
            }
            else -> null
        }
    }

    fun offlineReply(rawText: String): String {
        val text = rawText.trim().lowercase(Locale.ROOT)
        if (text.isBlank()) {
            return "Pode me mandar uma mensagem. No modo local, use /ajuda, /data, /memorias ou /resumo."
        }
        if (text.matches(Regex("^(oi|olá|ola|e aí|eai|bom dia|boa tarde|boa noite|opa)[!. ,]*$"))) {
            return "Oi! Sou o COPY. Estou funcionando em modo local: consigo executar /ajuda, /data, /memorias e /resumo sem internet. Para gerar respostas livres com IA, configure um modelo em Ajustes."
        }
        if (text.contains("quem é você") || text.contains("quem e voce") || text.contains("o que você faz") || text.contains("o que voce faz")) {
            return "Sou o COPY, seu assistente pessoal. O histórico e as memórias ficam no aparelho. Sem um modelo configurado, só consigo executar comandos locais; configure um modelo em Ajustes para ativar respostas generativas."
        }
        return "Mensagem guardada no histórico local. Ainda não há um modelo de IA configurado, então não vou inventar uma resposta. Abra Ajustes, preencha o provedor e o modelo (há um atalho para Gemini com plano gratuito), adicione sua chave e toque em Testar conexão. Para ajuda sem internet, envie /ajuda."
    }
}
