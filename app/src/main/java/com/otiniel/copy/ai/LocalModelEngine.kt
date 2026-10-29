package com.otiniel.copy.ai

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ThinkingConfig
import com.otiniel.copy.data.ChatMessage
import com.otiniel.copy.data.MemoryEntry
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Local-only inference with the bundled Qwen3-0.6B LiteRT-LM model.
 *
 * The quantized weights are packaged inside the APK, then copied to private
 * app storage on first activation because LiteRT-LM loads a filesystem path.
 * Prompts, history, and optional memory context are never sent over the network.
 */
class LocalModelEngine(context: Context) {
    private val appContext = context.applicationContext
    private val modelFile = File(appContext.filesDir, "models/$MODEL_FILENAME")
    private val engineMutex = Mutex()

    @Volatile
    private var loadedEngine: Engine? = null

    fun isPrepared(): Boolean =
        modelFile.isFile && modelFile.length() > MINIMUM_VALID_MODEL_BYTES

    suspend fun prepareBundledModel(onProgress: (Int) -> Unit = {}) = withContext(Dispatchers.IO) {
        if (isPrepared()) {
            onProgress(100)
            return@withContext
        }

        val parent = modelFile.parentFile ?: error("Não foi possível criar a pasta local do modelo.")
        check(parent.exists() || parent.mkdirs()) { "Não foi possível reservar espaço para a IA local." }

        val temporary = File(parent, "$MODEL_FILENAME.copying")
        temporary.delete()
        val assetPath = "models/$MODEL_FILENAME"
        val totalBytes = runCatching {
            appContext.assets.openFd(assetPath).use { it.length }
        }.getOrDefault(-1L)

        try {
            appContext.assets.open(assetPath).buffered().use { input ->
                temporary.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE * 4)
                    var copied = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (count == 0) continue
                        output.write(buffer, 0, count)
                        copied += count
                        if (totalBytes > 0L) {
                            onProgress(((copied * 100L) / totalBytes).toInt().coerceIn(0, 99))
                        }
                    }
                }
            }

            check(temporary.length() > MINIMUM_VALID_MODEL_BYTES) {
                "O modelo embutido parece incompleto. Reinstale o APK do COPY."
            }
            if (modelFile.exists() && !modelFile.delete()) {
                error("Não foi possível substituir o arquivo antigo da IA local.")
            }
            check(temporary.renameTo(modelFile)) { "Não foi possível finalizar a instalação da IA local." }
            onProgress(100)
        } catch (error: Exception) {
            temporary.delete()
            throw error
        }
    }

    suspend fun reply(
        transcript: List<ChatMessage>,
        localMemories: List<MemoryEntry> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        check(isPrepared()) { "Ative primeiro a IA local embutida em Central de controle." }
        val latestUserIndex = transcript.indexOfLast { it.role == "user" }
        check(latestUserIndex >= 0) { "Envie uma mensagem para começar." }
        val userText = transcript[latestUserIndex].content
        check(userText.isNotBlank()) { "A mensagem está vazia." }

        val priorMessages = transcript
            .take(latestUserIndex)
            .takeLast(MAX_HISTORY_MESSAGES)
            .mapNotNull { message ->
                when (message.role) {
                    "user" -> Message.user(message.content)
                    "assistant" -> Message.model(message.content)
                    else -> null
                }
            }

        val memoryContext = localMemories.take(MAX_MEMORY_NOTES).joinToString("\n") { memory ->
            val title = memory.title.replace("\n", " ").take(80)
            val content = memory.content.replace("\n", " ").take(MAX_MEMORY_CHARS)
            "- $title: $content"
        }
        val systemInstruction = buildString {
            append(
                "Você é COPY, um assistente pessoal útil, honesto e claro. " +
                    "Responda em português do Brasil por padrão. " +
                    "Não afirme ter realizado ações no celular: ferramentas de sistema ainda podem não estar disponíveis. " +
                    "Se não souber algo, diga isso. Prefira respostas diretas e bem estruturadas."
            )
            if (memoryContext.isNotBlank()) {
                append("\n\nMemórias locais escolhidas pelo usuário, apenas como contexto: \n")
                append(memoryContext)
                append("\nNão invente detalhes sobre essas memórias.")
            }
        }

        val localEngine = getOrCreateEngine()
        // Qwen3's chat template expects at least one initial message when a
        // system instruction is provided. Seed a harmless local greeting on a
        // brand-new conversation to avoid an empty-message template failure.
        val safeInitialMessages = if (priorMessages.isEmpty()) {
            listOf(
                Message.user("Olá."),
                Message.model("Olá! Sou o COPY, seu assistente pessoal.")
            )
        } else {
            priorMessages
        }

        val config = ConversationConfig(
            systemInstruction = Contents.of(systemInstruction),
            initialMessages = safeInitialMessages,
            samplerConfig = SamplerConfig(topK = 40, topP = 0.9, temperature = 0.7),
            maxOutputToken = MAX_OUTPUT_TOKENS,
            thinkingConfig = ThinkingConfig(enableThinking = false, thinkingTokenBudget = 0)
        )

        localEngine.createConversation(config).use { conversation ->
            conversation.sendMessage(userText).text.trim().ifBlank {
                "Não consegui gerar uma resposta útil. Tente reformular a mensagem."
            }
        }
    }

    private suspend fun getOrCreateEngine(): Engine = engineMutex.withLock {
        loadedEngine?.let { return@withLock it }
        val engine = Engine(
            EngineConfig(
                modelPath = modelFile.absolutePath,
                backend = Backend.CPU(),
                cacheDir = appContext.cacheDir.absolutePath
            )
        )
        try {
            engine.initialize()
            loadedEngine = engine
            engine
        } catch (error: Exception) {
            runCatching { engine.close() }
            throw error
        }
    }

    companion object {
        const val MODEL_FILENAME = "Qwen3-0.6B_dynamic_wi4b32_afp32.litertlm"
        private const val MINIMUM_VALID_MODEL_BYTES = 250_000_000L
        private const val MAX_HISTORY_MESSAGES = 12
        private const val MAX_MEMORY_NOTES = 20
        private const val MAX_MEMORY_CHARS = 600
        private const val MAX_OUTPUT_TOKENS = 512
    }
}
