package com.otiniel.copy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.otiniel.copy.data.ChatMessage
import com.otiniel.copy.data.MemoryEntry
import com.otiniel.copy.ui.CopyViewModel

private val Night = Color(0xFF070B14)
private val Panel = Color(0xFF0D1424)
private val PanelLight = Color(0xFF111D32)
private val Blue = Color(0xFF4EA1FF)
private val Cyan = Color(0xFF72E4FF)
private val White = Color(0xFFF2F6FF)
private val Muted = Color(0xFF8998B3)
private val Line = Color(0xFF1D2B44)
private val Good = Color(0xFF74E5BF)

class CopyFigmaActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(7, 11, 20)
        window.navigationBarColor = android.graphics.Color.rgb(7, 11, 20)
        val viewModel = ViewModelProvider(this)[CopyViewModel::class.java]
        setContent { CopyFigmaApp(viewModel) }
    }
}

private enum class CopyPage { HOME, CHAT, MEMORY, SETTINGS }

@Composable
private fun CopyFigmaApp(viewModel: CopyViewModel) {
    var page by remember { mutableStateOf(CopyPage.HOME) }
    val messages by viewModel.messages.collectAsState()
    val memories by viewModel.memories.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val error by viewModel.error.collectAsState()
    val notice by viewModel.notice.collectAsState()

    MaterialTheme {
        Scaffold(
            containerColor = Night,
            contentWindowInsets = WindowInsets.navigationBars,
            bottomBar = {
                CopyBottomBar(page = page, onSelect = { page = it })
            }
        ) { insets ->
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Night)
                    .padding(insets)
            ) {
                when (page) {
                    CopyPage.HOME -> HomePage(
                        onChat = { page = CopyPage.CHAT },
                        onMemory = { page = CopyPage.MEMORY },
                        onSettings = { page = CopyPage.SETTINGS },
                        aiConfigured = settings.endpoint.isNotBlank() && settings.model.isNotBlank()
                    )
                    CopyPage.CHAT -> ChatPage(
                        messages = messages,
                        busy = busy,
                        error = error,
                        notice = notice,
                        onSend = viewModel::sendMessage,
                        onClearFeedback = viewModel::clearFeedback,
                        onSettings = { page = CopyPage.SETTINGS }
                    )
                    CopyPage.MEMORY -> MemoryPage(
                        memories = memories,
                        onAdd = viewModel::addMemory,
                        onRefresh = viewModel::refreshMemories
                    )
                    CopyPage.SETTINGS -> SettingsPage(
                        endpoint = settings.endpoint,
                        model = settings.model,
                        apiKey = settings.apiKey,
                        busy = busy,
                        error = error,
                        notice = notice,
                        onSave = { endpoint, model, key, shareMemories ->
                            viewModel.saveSettings(endpoint, model, key, shareMemories)
                        },
                        includeMemories = settings.includeMemories,
                        onTest = viewModel::testConnection,
                        onClearFeedback = viewModel::clearFeedback
                    )
                }
            }
        }
    }
}

@Composable
private fun HomePage(
    onChat: () -> Unit,
    onMemory: () -> Unit,
    onSettings: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(Blue, Color(0xFF354CF5)))),
                contentAlignment = Alignment.Center
            ) {
                Text("C", color = White, fontWeight = FontWeight.Black, fontSize = 24.sp)
            }
            Spacer(Modifier.width(11.dp))
            Column {
                Text("COPY", color = White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp)
                Text("PERSONAL INTELLIGENCE", color = Muted, fontSize = 8.sp, letterSpacing = 1.5.sp)
            }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.clip(CircleShape).background(Panel).border(1.dp, Line, CircleShape).padding(10.dp)
            ) {
                Icon(Icons.Filled.Tune, contentDescription = "Configurações", tint = Muted, modifier = Modifier.size(20.dp))
                Box(Modifier.align(Alignment.TopEnd).size(6.dp).clip(CircleShape).background(Cyan))
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(PanelLight).border(1.dp, Line, CircleShape),
                contentAlignment = Alignment.Center
            ) { Text("O", color = Cyan, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }

        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(Good))
                Text("SEU ASSISTENTE, NO SEU RITMO", color = Good, fontSize = 9.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(
                "Uma nova forma\nde pensar com você.",
                color = White, fontSize = 35.sp, lineHeight = 39.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1.1).sp
            )
            Text(
                "Ideias, tarefas e conversas em um só lugar.\nCom você no controle.",
                color = Muted, fontSize = 14.sp, lineHeight = 21.sp
            )
        }

        Box(
            Modifier.fillMaxWidth().height(236.dp).clip(RoundedCornerShape(28.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF101E38), Color(0xFF09101D))))
                .border(1.dp, Color(0xFF233A5B), RoundedCornerShape(28.dp))
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x554EA1FF), Color(0x224E7BFF), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.48f),
                        radius = size.minDimension * 0.60f
                    ),
                    radius = size.minDimension * 0.60f,
                    center = Offset(size.width * 0.5f, size.height * 0.48f)
                )
                drawCircle(Color(0x224EA1FF), radius = size.minDimension * 0.38f, center = Offset(size.width * .5f, size.height * .48f), style = Stroke(width = 1.dp.toPx()))
            }
            OrbGraphic(Modifier.align(Alignment.Center).size(172.dp))
            Row(Modifier.align(Alignment.TopStart).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = Cyan, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(6.dp))
                Text("PRIVACIDADE EM PRIMEIRO LUGAR", color = Color(0xFFBDD7F5), fontSize = 8.sp, letterSpacing = 1.sp)
            }
            Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                Text("COPY CORE", color = White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                Text("Um espaço só seu.", color = Muted, fontSize = 11.sp)
            }
            Box(
                Modifier.align(Alignment.BottomEnd).padding(16.dp).clip(CircleShape)
                    .background(Color(0x223F9BFF)).border(1.dp, Color(0x665DAEFF), CircleShape).padding(10.dp)
            ) { Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = Cyan, modifier = Modifier.size(17.dp)) }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("O que vamos fazer?", color = White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text("SEU ESPAÇO", color = Muted, fontSize = 9.sp, letterSpacing = 1.sp)
            }
            ActionCard(
                icon = { Icon(Icons.Filled.ChatBubbleOutline, contentDescription = null, tint = Cyan, modifier = Modifier.size(21.dp)) },
                title = "Começar uma conversa",
                subtitle = "Ideias, perguntas e novos caminhos",
                onClick = onChat,
                highlighted = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmallActionCard(
                    modifier = Modifier.weight(1f),
                    icon = { Icon(Icons.Filled.BookmarkBorder, contentDescription = null, tint = Blue, modifier = Modifier.size(20.dp)) },
                    title = "Memória",
                    subtitle = "Suas anotações",
                    onClick = onMemory
                )
                SmallActionCard(
                    modifier = Modifier.weight(1f),
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null, tint = Blue, modifier = Modifier.size(20.dp)) },
                    title = "Conexões",
                    subtitle = "Configure sua IA",
                    onClick = onSettings
                )
            }
        }

        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0B1725)).border(1.dp, Color(0xFF183148), RoundedCornerShape(16.dp)).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = Good, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Seus dados ficam com você", color = White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("Histórico e memória são armazenados localmente.", color = Muted, fontSize = 10.sp, lineHeight = 15.sp)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Muted, modifier = Modifier.size(17.dp))
        }
    }
}

@Composable
private fun OrbGraphic(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "copy-orb")
    val spin by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(16000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Restart),
        label = "orbit"
    )
    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF9DEEFF), Color(0xFF4EA1FF), Color(0xFF244BBD), Color(0xFF0B1638)),
                center = Offset(size.width * .36f, size.height * .32f),
                radius = size.minDimension * .62f
            ),
            radius = size.minDimension * .32f,
            center = center
        )
        drawCircle(Color(0x8859C9FF), radius = size.minDimension * .34f, center = center, style = Stroke(width = 1.2.dp.toPx()))
        rotate(spin, pivot = center) {
            drawOval(
                color = Color(0xAA8DE8FF),
                topLeft = Offset(size.width * .05f, size.height * .35f),
                size = Size(size.width * .90f, size.height * .30f),
                style = Stroke(width = 1.2.dp.toPx())
            )
        }
        rotate(-spin * .65f, pivot = center) {
            drawOval(
                color = Color(0x7771AFFF),
                topLeft = Offset(size.width * .16f, size.height * .18f),
                size = Size(size.width * .68f, size.height * .64f),
                style = Stroke(width = 1.dp.toPx())
            )
        }
        drawCircle(Color(0xFFDBF9FF), radius = 3.dp.toPx(), center = Offset(size.width * .68f, size.height * .25f))
    }
}

@Composable
private fun ActionCard(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    highlighted: Boolean = false
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(if (highlighted) Brush.horizontalGradient(listOf(Color(0xFF132849), Color(0xFF0E1A2D))) else Brush.horizontalGradient(listOf(Panel, Panel)))
            .border(1.dp, if (highlighted) Color(0xFF294D7B) else Line, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color(0x223F91E8)), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Muted, fontSize = 11.sp)
        }
        Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = if (highlighted) Cyan else Muted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SmallActionCard(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column(
        modifier.clip(RoundedCornerShape(20.dp)).background(Panel).border(1.dp, Line, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick).padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x223F91E8)), contentAlignment = Alignment.Center) { icon() }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Muted, fontSize = 10.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun ChatPage(
    messages: List<ChatMessage>,
    busy: Boolean,
    error: String?,
    notice: String?,
    onSend: (String) -> Unit,
    onClearFeedback: () -> Unit,
    onSettings: () -> Unit,
    aiConfigured: Boolean
) {
    var draft by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().imePadding().padding(horizontal = 16.dp)) {
        PageHeader("Conversa", "Seu espaço de ideias") { onSettings() }
        if (error != null || notice != null) {
            FeedbackCard(message = error ?: notice.orEmpty(), isError = error != null, onDismiss = onClearFeedback)
        }
        if (messages.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OrbGraphic(Modifier.size(106.dp))
                    Text("O que está na sua mente?", color = White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text("Comece com uma pergunta ou uma ideia.", color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                reverseLayout = true
            ) {
                items(messages.reversed(), key = { it.id }) { message ->
                    MessageItem(message)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 12.dp).clip(RoundedCornerShape(22.dp))
                .background(Panel).border(1.dp, Line, RoundedCornerShape(22.dp)).padding(start = 8.dp, end = 7.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { draft += "" }, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Filled.Add, contentDescription = "Adicionar", tint = Muted)
            }
            TextField(
                value = draft, onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Escreva sua mensagem...", color = Muted, fontSize = 13.sp) },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = White, unfocusedTextColor = White,
                    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Cyan
                ),
                maxLines = 4
            )
            IconButton(
                onClick = { val text = draft.trim(); if (text.isNotEmpty() && !busy) { onSend(text); draft = "" } },
                enabled = draft.isNotBlank() && !busy,
                modifier = Modifier.size(40.dp).clip(CircleShape).background(if (draft.isNotBlank() && !busy) Blue else PanelLight)
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Enviar", tint = if (draft.isNotBlank() && !busy) Night else Muted, modifier = Modifier.size(18.dp))
            }
        }
        Text(
            if (busy) "COPY está preparando uma resposta…" else if (aiConfigured) "Modelo configurado · as mensagens seguem para o provedor escolhido." else "Modo local · /ajuda, /data, /memorias e /resumo funcionam sem internet.",
            color = Muted, fontSize = 9.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )
    }
}

@Composable
private fun MessageItem(message: ChatMessage) {
    val user = message.role == "user"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (user) Arrangement.End else Arrangement.Start) {
        Column(
            Modifier.fillMaxWidth(.90f).clip(
                RoundedCornerShape(topStart = 17.dp, topEnd = 17.dp, bottomStart = if (user) 17.dp else 5.dp, bottomEnd = if (user) 5.dp else 17.dp)
            ).background(if (user) Color(0xFF18345A) else Panel).border(1.dp, if (user) Color(0xFF2B548A) else Line, RoundedCornerShape(17.dp)).padding(14.dp)
        ) {
            Text(if (user) "VOCÊ" else "COPY", color = if (user) Cyan else Blue, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(6.dp))
            Text(message.content, color = White, fontSize = 13.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun MemoryPage(
    memories: List<MemoryEntry>,
    onAdd: (String) -> Unit,
    onRefresh: (String) -> Unit
) {
    var draft by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        PageHeader("Memória", "O que você escolhe guardar") { onRefresh("") }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Panel).border(1.dp, Line, RoundedCornerShape(20.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = draft, onValueChange = { draft = it }, modifier = Modifier.weight(1f),
                placeholder = { Text("Salvar uma lembrança…", color = Muted, fontSize = 13.sp) },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = White, unfocusedTextColor = White,
                    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Cyan
                ), maxLines = 3
            )
            IconButton(onClick = { if (draft.isNotBlank()) { onAdd(draft); draft = "" } }) {
                Icon(Icons.Filled.Add, contentDescription = "Salvar memória", tint = Cyan)
            }
        }
        Text("SALVAS NESTE APARELHO", color = Muted, fontSize = 9.sp, letterSpacing = 1.2.sp, modifier = Modifier.padding(top = 20.dp, bottom = 10.dp))
        if (memories.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.BookmarkBorder, contentDescription = null, tint = Blue, modifier = Modifier.size(38.dp))
                    Text("Sua memória começa aqui.", color = White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text("Adicione algo que queira manter por perto.", color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(memories, key = { it.id }) { memory ->
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Panel).border(1.dp, Line, RoundedCornerShape(17.dp)).padding(15.dp)) {
                        Text(memory.title, color = White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text(memory.content, color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
                    }
                }
            }
        }
        Text("Você decide o que entra na memória.", color = Muted, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 10.dp))
    }
}

@Composable
private fun SettingsPage(
    endpoint: String,
    model: String,
    apiKey: String,
    busy: Boolean,
    error: String?,
    notice: String?,
    onSave: (String, String, String, Boolean) -> Unit,
    includeMemories: Boolean,
    onTest: () -> Unit,
    onClearFeedback: () -> Unit
) {
    var endpointInput by remember(endpoint) { mutableStateOf(endpoint) }
    var modelInput by remember(model) { mutableStateOf(model) }
    var keyInput by remember(apiKey) { mutableStateOf(apiKey) }
    var includeMemoriesInput by remember(includeMemories) { mutableStateOf(includeMemories) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp)) {
        PageHeader("Central de controle", "Você escolhe como o COPY funciona") { onClearFeedback() }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Color(0xFF0B1725)).border(1.dp, Color(0xFF183148), RoundedCornerShape(17.dp)).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = Good, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Conexão opcional", color = White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("As configurações são guardadas cifradas no aparelho.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
        Spacer(Modifier.height(22.dp))
        Text("PROVEDOR COMPATÍVEL COM OPENAI", color = Muted, fontSize = 9.sp, letterSpacing = 1.2.sp)
        Spacer(Modifier.height(6.dp))
        TextButton(
            onClick = {
                endpointInput = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"
                modelInput = "gemini-3.1-flash-lite"
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Preencher Gemini 3.1 Flash-Lite (plano gratuito)", color = Cyan, fontSize = 12.sp)
        }
        Text(
            "Depois, crie uma chave em aistudio.google.com/apikey, cole no campo e salve. O plano gratuito tem limites; consulte as regras de privacidade do provedor antes de enviar dados.",
            color = Muted, fontSize = 10.sp, lineHeight = 15.sp
        )
        Spacer(Modifier.height(10.dp))
        ControlField("Endpoint HTTPS", endpointInput, { endpointInput = it }, "https://seu-servidor/v1/chat/completions")
        ControlField("Identificador do modelo", modelInput, { modelInput = it }, "modelo-que-vai-usar")
        ControlField("Chave de API", keyInput, { keyInput = it }, "Cole sua chave aqui", isSecret = true)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = includeMemoriesInput,
                onCheckedChange = { includeMemoriesInput = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = Blue,
                    uncheckedColor = Muted,
                    checkmarkColor = Night
                )
            )
            Spacer(Modifier.width(5.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Permitir que a IA use minhas memórias", color = White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text("Desativado por padrão. Quando ativado, as notas salvas podem ser enviadas ao provedor junto com sua mensagem.", color = Muted, fontSize = 10.sp, lineHeight = 14.sp)
            }
        }
        if (error != null || notice != null) {
            Spacer(Modifier.height(10.dp))
            FeedbackCard(message = error ?: notice.orEmpty(), isError = error != null, onDismiss = onClearFeedback)
        }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = { onSave(endpointInput, modelInput, keyInput, includeMemoriesInput) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Blue, contentColor = Night),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Salvar configuração", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(9.dp))
        Button(
            onClick = onTest,
            enabled = !busy && endpointInput.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PanelLight, contentColor = White),
            shape = RoundedCornerShape(16.dp)
        ) { Text(if (busy) "Testando…" else "Testar conexão") }
        TextButton(onClick = { endpointInput = ""; modelInput = ""; keyInput = ""; includeMemoriesInput = false; onSave("", "", "", false) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Remover configuração", color = Muted, fontSize = 12.sp)
        }
        Text("O COPY não inclui um modelo de IA pago. Você pode usar um provedor seu ou manter a experiência sem conexão externa.", color = Muted, fontSize = 11.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 8.dp, bottom = 20.dp))
    }
}

@Composable
private fun ControlField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isSecret: Boolean = false
) {
    Column(Modifier.fillMaxWidth().padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, color = White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        OutlinedTextField(
            value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = Muted, fontSize = 11.sp) },
            singleLine = true,
            visualTransformation = if (isSecret) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = White, unfocusedTextColor = White,
                focusedBorderColor = Blue, unfocusedBorderColor = Line,
                focusedContainerColor = Panel, unfocusedContainerColor = Panel,
                cursorColor = Cyan
            ),
            shape = RoundedCornerShape(15.dp)
        )
    }
}

@Composable
private fun FeedbackCard(message: String, isError: Boolean, onDismiss: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(13.dp))
            .background(if (isError) Color(0xFF351820) else Color(0xFF0D2A28))
            .clickable(onClick = onDismiss).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = if (isError) Color(0xFFFF8097) else Good, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(8.dp))
        Text(message, color = White, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PageHeader(title: String, subtitle: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 9.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Muted, fontSize = 11.sp)
        }
        IconButton(onClick = onAction) {
            Icon(Icons.Filled.Tune, contentDescription = "Ações", tint = Muted)
        }
    }
}

@Composable
private fun CopyBottomBar(page: CopyPage, onSelect: (CopyPage) -> Unit) {
    Surface(color = Color(0xFF090E18), shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 7.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem("Início", Icons.Filled.Home, page == CopyPage.HOME) { onSelect(CopyPage.HOME) }
            BottomNavItem("Conversa", Icons.Filled.ChatBubbleOutline, page == CopyPage.CHAT) { onSelect(CopyPage.CHAT) }
            BottomNavItem("Memória", Icons.Filled.BookmarkBorder, page == CopyPage.MEMORY) { onSelect(CopyPage.MEMORY) }
            BottomNavItem("Ajustes", Icons.Filled.Settings, page == CopyPage.SETTINGS) { onSelect(CopyPage.SETTINGS) }
        }
    }
}

@Composable
private fun BottomNavItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(13.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(icon, contentDescription = label, tint = if (selected) Cyan else Muted, modifier = Modifier.size(20.dp))
        Text(label, color = if (selected) White else Muted, fontSize = 9.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}
