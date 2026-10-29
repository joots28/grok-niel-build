package com.otiniel.copy.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.otiniel.copy.ai.EndpointUrl
import com.otiniel.copy.ai.ModelRouter
import com.otiniel.copy.ai.LocalModelEngine
import com.otiniel.copy.data.ChatMessage
import com.otiniel.copy.data.ConnectionSettings
import com.otiniel.copy.data.CopyDatabase
import com.otiniel.copy.data.MemoryEntry
import com.otiniel.copy.data.SecureSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CopyViewModel(application: Application) : AndroidViewModel(application) {
    private val database = CopyDatabase(application.applicationContext)
    private val secureSettingsStore = SecureSettingsStore(application.applicationContext)
    private val localModelEngine = LocalModelEngine(application.applicationContext)
    private val modelRouter = ModelRouter(localModelEngine = localModelEngine)

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _memories = MutableStateFlow<List<MemoryEntry>>(emptyList())
    val memories: StateFlow<List<MemoryEntry>> = _memories.asStateFlow()

    private val _settings = MutableStateFlow(ConnectionSettings())
    val settings: StateFlow<ConnectionSettings> = _settings.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    private val _localModelReady = MutableStateFlow(localModelEngine.isPrepared())
    val localModelReady: StateFlow<Boolean> = _localModelReady.asStateFlow()

    private val _localModelPreparing = MutableStateFlow(false)
    val localModelPreparing: StateFlow<Boolean> = _localModelPreparing.asStateFlow()

    private val _localModelProgress = MutableStateFlow(0)
    val localModelProgress: StateFlow<Int> = _localModelProgress.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            database.ensureWelcomeMessage()
            _messages.value = database.getMessages()
            _memories.value = database.getMemories()
            _settings.value = secureSettingsStore.load()
        }
    }

    fun prepareLocalAi() {
        if (_localModelPreparing.value || _localModelReady.value) return
        _error.value = null
        _notice.value = null
        viewModelScope.launch(Dispatchers.IO) {
            _localModelPreparing.value = true
            _localModelProgress.value = 0
            try {
                localModelEngine.prepareBundledModel { percent ->
                    _localModelProgress.value = percent
                }
                _localModelReady.value = true
                _notice.value = "IA local ativada. O COPY pode conversar sem chave de API e sem internet."
            } catch (error: Exception) {
                _error.value = error.message ?: "Não foi possível preparar a IA local."
            } finally {
                _localModelPreparing.value = false
            }
        }
    }

    fun sendMessage(rawText: String) {
        val text = rawText.trim()
        if (text.isBlank() || _busy.value) return

        _error.value = null
        _notice.value = null
        viewModelScope.launch {
            _busy.value = true
            try {
                val reply = withContext(Dispatchers.IO) {
                    database.ensureWelcomeMessage()
                    database.insertMessage("user", text)
                    _messages.value = database.getMessages()
                    val localMemories = database.getMemories()
                    val result = runCatching { modelRouter.reply(_settings.value, _messages.value, localMemories) }
                    result.onFailure { error ->
                        _error.value = userFriendlyError(error)
                    }.getOrElse { error ->
                        "Não consegui concluir a solicitação. " +
                            (error.message?.take(180) ?: "Verifique a conexão e a configuração do provedor.")
                    }
                }
                withContext(Dispatchers.IO) {
                    database.insertMessage("assistant", reply)
                    _messages.value = database.getMessages()
                }
            } finally {
                _busy.value = false
            }
        }
    }

    fun saveSettings(endpoint: String, model: String, apiKey: String) =
        saveSettings(endpoint, model, apiKey, _settings.value.includeMemories)

    fun saveSettings(endpoint: String, model: String, apiKey: String, includeMemories: Boolean) {
        _error.value = null
        _notice.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cleanEndpoint = endpoint.trim()
                val cleanModel = model.trim()
                if (cleanEndpoint.isNotEmpty()) {
                    EndpointUrl.chatCompletions(cleanEndpoint)
                    require(cleanModel.isNotBlank()) { "Informe o identificador do modelo." }
                }
                val next = if (cleanEndpoint.isBlank()) {
                    ConnectionSettings()
                } else {
                    ConnectionSettings(cleanEndpoint, cleanModel, apiKey.trim(), includeMemories)
                }
                secureSettingsStore.save(next)
                _settings.value = next
                _notice.value = if (cleanEndpoint.isBlank()) {
                    "Configuração removida. O chat não enviará mensagens a um provedor externo."
                } else {
                    "Configuração salva de forma cifrada no aparelho."
                }
            } catch (error: Exception) {
                _error.value = error.message ?: "Não foi possível salvar a configuração."
            }
        }
    }

    fun testConnection() {
        if (_busy.value) return
        _error.value = null
        _notice.value = null
        viewModelScope.launch {
            _busy.value = true
            try {
                val result = withContext(Dispatchers.IO) { modelRouter.testConnection(_settings.value) }
                _notice.value = "Resposta de teste: " + result.take(120)
            } catch (error: Exception) {
                _error.value = userFriendlyError(error)
            } finally {
                _busy.value = false
            }
        }
    }

    fun addMemory(rawContent: String) {
        val content = rawContent.trim()
        if (content.isBlank()) return
        _error.value = null
        _notice.value = null
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val title = content.lineSequence().firstOrNull().orEmpty().take(48)
                database.insertMemory(title, content)
                _memories.value = database.getMemories()
            }.onSuccess {
                _notice.value = "Memória salva apenas neste aparelho."
            }.onFailure {
                _error.value = it.message ?: "Não foi possível salvar a memória."
            }
        }
    }

    fun refreshMemories(search: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            _memories.value = database.getMemories(search)
        }
    }

    fun clearFeedback() {
        _error.value = null
        _notice.value = null
    }

    private fun userFriendlyError(error: Throwable): String {
        val message = error.message?.takeIf { it.isNotBlank() }
        return "COPY não conseguiu responder: " + (message ?: "erro inesperado. Verifique a configuração.")
    }
}
