package com.otiniel.copy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.otiniel.copy.data.ChatMessage
import com.otiniel.copy.data.ConnectionSettings
import com.otiniel.copy.data.MemoryEntry
import com.otiniel.copy.ui.CopyViewModel

private val CopyBackground = Color(0xFF060A14)
private val CopySurface = Color(0xFF0D1422)
private val CopySurfaceRaised = Color(0xFF121D30)
private val CopyBlue = Color(0xFF56B6FF)
private val CopyViolet = Color(0xFF8F85FF)
private val CopyText = Color(0xFFEAF1FC)
private val CopyMuted = Color(0xFF8C9CB5)
private val CopyBorder = Color(0xFF24334A)

class MainActivity : ComponentActivity() {
    private lateinit var copyViewModel: CopyViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(6, 10, 20)
        window.navigationBarColor = android.graphics.Color.rgb(6, 10, 20)
        copyViewModel = ViewModelProvider(this)[CopyViewModel::class.java]
        setContent { CopyTheme { CopyApp(copyViewModel) } }
    }
}

private enum class CopyScreen { CHAT, MEMORY, SETTINGS }

@Composable
private fun CopyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = CopyBlue,
            onPrimary = CopyBackground,
            secondary = CopyViolet,
            background = CopyBackground,
            surface = CopySurface,
            surfaceVariant = CopySurfaceRaised,
            onBackground = CopyText,
            onSurface = CopyText,
            onSurfaceVariant = CopyMuted,
            error = Color(0xFFFF7485)
        ),
        content = content
    )
}

@Composable
private fun CopyApp(viewModel: CopyViewModel) {
    val messages by viewModel.messages.collectAsState()
    val memories by viewModel.memories.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val error by viewModel.error.collectAsState()
    val notice by viewModel.notice.collectAsState()
    var screen by remember { mutableStateOf(CopyScreen.CHAT) }

    Scaffold(
        containerColor = CopyBackground,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF090F1B), tonalElevation = 0.dp) {
                NavigationBarItem(
                    selected = screen == CopyScreen.CHAT,
                    onClick = { screen = CopyScreen.CHAT },
                    icon = { Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Conversa") },
                    label = { Text("Conversa") },
                    colors = navColors()
                )
                NavigationBarItem(
                    selected = screen == CopyScreen.MEMORY,
                    onClick = { screen = CopyScreen.MEMORY },
                    icon = { Icon(Icons.Outlined.BookmarkBorder, contentDescription = "Memória") },
                    label = { Text("Memória") },
                    colors = navColors()
                )
                NavigationBarItem(
                    selected = screen == CopyScreen.SETTINGS,
                    onClick = { screen = CopyScreen.SETTINGS },
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = "Configurações") },
                    label = { Text("Config.") },
                    colors = navColors()
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Brush.verticalGradient(listOf(Color(0xFF080E1B), CopyBackground, Color(0xFF050811))))
        ) {
            when (screen) {
                CopyScreen.CHAT -> ChatScreen(
                    messages = messages,
                    busy = busy,
                    feedback = error ?: notice,
                    isError = error != null,
                    onSend = viewModel::sendMessage,
                    onInputChanged = viewModel::clearFeedback
                )
                CopyScreen.MEMORY -> MemoryScreen(
                    memories = memories,
                    feedback = error ?: notice,
                    isError = error != null,
                    onAddMemory = viewModel::addMemory
                )
                CopyScreen.SETTINGS -> SettingsScreen(
                    settings = settings,
                    busy = busy,
                    feedback = error ?: notice,
                    isError = error != null,
                    onSave = viewModel::saveSettings,
                    onTest = viewModel::testConnection
                )
            }
        }
    }
}

@Composable
private fun navColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = CopyBlue,
    selectedTextColor = CopyBlue,
    indicatorColor = Color(0xFF192A41),
    unselectedIconColor = CopyMuted,
    unselectedTextColor = CopyMuted
)

@Composable
private fun ChatScreen(
    messages: List<ChatMessage>,
    busy: Boolean,
    feedback: String?,
    isError: Boolean,
    onSend: (String) -> Unit,
    onInputChanged: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, busy) {
        val lastItem = messages.lastIndex + if (busy) 1 else 0
        if (lastItem >= 0) listState.animateScrollToItem(lastItem)
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        HeaderBlock(
            eyebrow = "ASSISTENTE PESSOAL",
            title = "COPY",
            subtitle = "Dados locais · IA configurável"
        )
        feedback?.let { FeedbackBanner(it, isError) }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(messages, key = { it.id }) { message -> MessageBubble(message) }
            if (busy) {
                item(key = "copy-thinking") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OrbBadge(size = 30.dp)
                        Spacer(Modifier.size(9.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = CopyBlue,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.size(8.dp))
                        Text("Processando solicitação…", color = CopyMuted, fontSize = 13.sp)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 14.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it; onInputChanged() },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Escreva para o COPY…", color = CopyMuted) },
                maxLines = 4,
                shape = RoundedCornerShape(22.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    val cleaned = input.trim()
                    if (cleaned.isNotEmpty() && !busy) { onSend(cleaned); input = "" }
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CopyBlue,
                    unfocusedBorderColor = CopyBorder,
                    focusedContainerColor = CopySurface,
                    unfocusedContainerColor = CopySurface,
                    cursorColor = CopyBlue
                )
            )
            Spacer(Modifier.size(8.dp))
            IconButton(
                enabled = input.isNotBlank() && !busy,
                onClick = {
                    val cleaned = input.trim()
                    if (cleaned.isNotEmpty()) { onSend(cleaned); input = "" }
                },
                modifier = Modifier.size(54.dp).clip(CircleShape)
                    .background(if (input.isNotBlank() && !busy) CopyBlue else CopySurfaceRaised)
            ) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = CopyText, strokeWidth = 2.dp)
                } else {
                    Icon(
                        Icons.Outlined.Send,
                        contentDescription = "Enviar mensagem",
                        tint = if (input.isNotBlank()) CopyBackground else CopyMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderBlock(eyebrow: String, title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OrbBadge(size = 44.dp)
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(eyebrow, color = CopyBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
            Text(title, color = CopyText, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.2.sp)
            Text(subtitle, color = CopyMuted, fontSize = 12.sp)
        }
        Surface(
            color = Color(0xFF0D1B2B),
            shape = RoundedCornerShape(50),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3A55))
        ) {
            Text(
                "ALFA",
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                color = CopyBlue,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp
            )
        }
    }
}

@Composable
private fun OrbBadge(size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val radius = this.size.minDimension * 0.32f
        drawCircle(color = CopyBlue.copy(alpha = 0.20f), radius = this.size.minDimension * 0.48f)
        drawCircle(
            color = CopyBlue,
            radius = this.size.minDimension * 0.44f,
            style = Stroke(width = this.size.minDimension * 0.035f)
        )
        drawOval(
            color = CopyViolet.copy(alpha = 0.9f),
            topLeft = Offset(this.size.width * 0.12f, this.size.height * 0.32f),
            size = Size(this.size.width * 0.76f, this.size.height * 0.36f),
            style = Stroke(width = this.size.minDimension * 0.035f)
        )
        drawCircle(color = Color.White, radius = radius * 0.28f, center = center)
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            OrbBadge(size = 30.dp)
            Spacer(Modifier.size(8.dp))
        }
        Card(
            modifier = Modifier.widthIn(max = 320.dp),
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 5.dp,
                bottomEnd = if (isUser) 5.dp else 20.dp
            ),
            colors = CardDefaults.cardColors(containerColor = if (isUser) Color(0xFF173B5D) else CopySurface),
            border = if (isUser) null else androidx.compose.foundation.BorderStroke(1.dp, CopyBorder)
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text(
                    if (isUser) "VOCÊ" else "COPY",
                    color = if (isUser) Color(0xFF91D5FF) else CopyBlue,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp
                )
                Spacer(Modifier.height(5.dp))
                Text(message.content, color = CopyText, fontSize = 14.sp, lineHeight = 21.sp)
            }
        }
    }
}

@Composable
private fun MemoryScreen(
    memories: List<MemoryEntry>,
    feedback: String?,
    isError: Boolean,
    onAddMemory: (String) -> Unit
) {
    var draft by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    val filtered = remember(memories, search) {
        if (search.isBlank()) memories
        else memories.filter {
            it.title.contains(search, ignoreCase = true) || it.content.contains(search, ignoreCase = true)
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        HeaderBlock("PRIVACIDADE PRIMEIRO", "MEMÓRIA", "Você decide o que fica salvo")
        feedback?.let { FeedbackBanner(it, isError) }

        Card(
            colors = CardDefaults.cardColors(containerColor = CopySurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, CopyBorder),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(Modifier.padding(15.dp)) {
                Text("Adicionar memória", color = CopyText, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Algo que você quer que o COPY lembre…") },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CopyBlue,
                        unfocusedBorderColor = CopyBorder,
                        focusedContainerColor = CopyBackground,
                        unfocusedContainerColor = CopyBackground
                    )
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { if (draft.isNotBlank()) { onAddMemory(draft); draft = "" } },
                    enabled = draft.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = CopyBlue),
                    shape = RoundedCornerShape(13.dp)
                ) {
                    Icon(Icons.Outlined.BookmarkBorder, contentDescription = null)
                    Spacer(Modifier.size(7.dp))
                    Text("Salvar no aparelho", color = CopyBackground)
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Buscar nas memórias…") },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CopyBlue,
                unfocusedBorderColor = CopyBorder,
                focusedContainerColor = CopySurface,
                unfocusedContainerColor = CopySurface
            )
        )
        Spacer(Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            Text(
                "Nenhuma memória encontrada. As memórias são adicionadas manualmente; nada é guardado automaticamente aqui.",
                color = CopyMuted,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filtered, key = { it.id }) { memory -> MemoryCard(memory) }
            }
        }
    }
}

@Composable
private fun MemoryCard(memory: MemoryEntry) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CopySurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, CopyBorder),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.BookmarkBorder, contentDescription = null, tint = CopyBlue)
                Spacer(Modifier.size(8.dp))
                Text(memory.title, color = CopyText, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(7.dp))
            Text(memory.content, color = CopyMuted, fontSize = 13.sp, lineHeight = 19.sp)
        }
    }
}

@Composable
private fun SettingsScreen(
    settings: ConnectionSettings,
    busy: Boolean,
    feedback: String?,
    isError: Boolean,
    onSave: (String, String, String) -> Unit,
    onTest: () -> Unit
) {
    var endpoint by remember(settings.endpoint) { mutableStateOf(settings.endpoint) }
    var model by remember(settings.model) { mutableStateOf(settings.model) }
    var apiKey by remember(settings.apiKey) { mutableStateOf(settings.apiKey) }

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        HeaderBlock("CONTROLE DO USUÁRIO", "CONFIGURAÇÕES", "Nenhum provedor vem pré-configurado")
        feedback?.let { FeedbackBanner(it, isError) }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 18.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CopySurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CopyBorder),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Lock, contentDescription = null, tint = CopyBlue)
                            Spacer(Modifier.size(8.dp))
                            Text("Conexão de IA opcional", color = CopyText, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Informe um endpoint HTTPS compatível com chat completions. Use um serviço gratuito, seu próprio servidor ou outro provedor. Custos, limites e políticas dependem do serviço escolhido.",
                            color = CopyMuted,
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )
                        SettingField(endpoint, { endpoint = it }, "Endpoint HTTPS", "https://provedor.example/v1", KeyboardType.Uri)
                        SettingField(model, { model = it }, "Identificador do modelo", "nome-do-modelo")
                        SettingField(
                            apiKey,
                            { apiKey = it },
                            "Chave de API (opcional)",
                            "A chave fica cifrada no aparelho",
                            KeyboardType.Password,
                            password = true
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { onSave(endpoint, model, apiKey) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = CopyBlue),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Salvar configuração", color = CopyBackground, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = onTest,
                            enabled = !busy && settings.endpoint.isNotBlank() && settings.model.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = CopySurfaceRaised, contentColor = CopyText),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (busy) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = CopyBlue)
                                Spacer(Modifier.size(8.dp))
                            }
                            Text("Testar configuração salva")
                        }
                    }
                }
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1822)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1C4256)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            "SEGURANÇA E PRIVACIDADE",
                            color = CopyBlue,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(Modifier.height(7.dp))
                        Text(
                            "• Histórico e memórias ficam no SQLite local.\n• A chave é cifrada com AES-GCM e protegida pelo Android Keystore.\n• Mensagens só são enviadas ao provedor após você configurar e enviar uma solicitação.\n• Ações no Android ainda não estão habilitadas nesta versão.",
                            color = CopyMuted,
                            fontSize = 12.sp,
                            lineHeight = 19.sp
                        )
                    }
                }
            }
            item {
                Text(
                    "Para desconectar, deixe o endpoint vazio e salve. Não compartilhe nem publique sua chave de API.",
                    color = CopyMuted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    password: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().padding(top = 9.dp),
        label = { Text(label) },
        placeholder = { Text(placeholder, color = CopyMuted) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        shape = RoundedCornerShape(15.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CopyBlue,
            unfocusedBorderColor = CopyBorder,
            focusedContainerColor = CopyBackground,
            unfocusedContainerColor = CopyBackground,
            cursorColor = CopyBlue
        )
    )
}

@Composable
private fun FeedbackBanner(text: String, isError: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        color = if (isError) Color(0xFF321821) else Color(0xFF102B3A),
        contentColor = if (isError) Color(0xFFFFB0BC) else Color(0xFFB7E7FF),
        shape = RoundedCornerShape(13.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isError) Color(0xFF763044) else Color(0xFF1E536D)
        )
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), fontSize = 12.sp, lineHeight = 17.sp)
    }
}
