package com.example.collegeadmin.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.collegeadmin.ai.AiAssistant
import com.example.collegeadmin.model.ChatMessage
import com.example.collegeadmin.model.UserInfo
import com.example.collegeadmin.ui.CollegeViewModel
import com.example.collegeadmin.ui.components.ScreenIndicator
import com.example.collegeadmin.ui.components.HelpPopup
import com.example.collegeadmin.ui.components.HelpItem
import kotlinx.coroutines.launch

@Composable
fun AiTutorScreen(viewModel: CollegeViewModel, paddingValues: PaddingValues) {
    val userInfo by viewModel.userInfo.collectAsState()
    val apiKey = "AIzaSyAQMyzTBaQ8zBy7J_-sRF84zpbuPhZHyNU"
    val assistant = remember { AiAssistant(apiKey) }
    val scope = rememberCoroutineScope()
    
    var inputText by remember { mutableStateOf("") }
    val messages by viewModel.chatMessages.collectAsState()
    var isTyping by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    var showHelp by remember { mutableStateOf(false) }

    LaunchedEffect(userInfo?.showHelp) {
        if (userInfo?.showHelp == true) {
            showHelp = true
        }
    }

    if (showHelp) {
        HelpPopup(
            title = "Tutor IA",
            helpItems = listOf(
                HelpItem(
                    "Converse com a IA",
                    "Faça qualquer pergunta sobre seus estudos. A IA usa o Gemini para te dar respostas, resumos e explicações.",
                    Icons.Default.QuestionAnswer,
                    Color(0xFF8B5CF6)
                ),
                HelpItem(
                    "Sugestões Rápidas",
                    "Use os botões de sugestão para iniciar uma conversa rapidamente, como pedir um quiz ou um plano de estudos.",
                    Icons.Default.Lightbulb,
                    Color(0xFF10B981)
                ),
                HelpItem(
                    "Aproveite o Poder da IA",
                    "A IA está integrada em todo o app, gerando resumos de provas e planos de estudo baseados no seu progresso.",
                    Icons.Default.AutoAwesome,
                    MaterialTheme.colorScheme.primary
                )
            ),
            onDismiss = { 
                showHelp = false
                viewModel.setShowHelp(false)
            }
        )
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val suggestionChips = listOf(
        "📝 Resumir aula",
        "❓ Criar quiz",
        "💡 Explicar tópico",
        "📅 Plano de estudo"
    )

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(paddingValues)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ScreenIndicator(label = "Tutor IA", color = Color(0xFF8B5CF6))
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { showHelp = true }) {
                            Icon(Icons.Default.HelpOutline, "Ajuda", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.weight(1f))
                        if (messages.isNotEmpty()) {
                            IconButton(onClick = { viewModel.clearChat() }) {
                                Icon(Icons.Default.DeleteSweep, "Limpar Chat", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)))), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text("Gemini Assistant", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                if (messages.isEmpty()) {
                    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Como posso ajudar hoje?", color = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.height(20.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(suggestionChips) { chip ->
                                SuggestionChipPremium(chip) { inputText = chip.substring(2) }
                            }
                        }
                    }
                } else {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(messages) { message -> 
                            MessageItem(message, userInfo)
                        }
                        if (isTyping) { item { TypingIndicator() } }
                    }
                }
            }

            Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
                Row(Modifier.padding(16.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextField(value = inputText, onValueChange = { inputText = it }, modifier = Modifier.weight(1f), placeholder = { Text("Mensagem...") }, colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
                    IconButton(onClick = {
                        if (inputText.isNotBlank()) {
                            val userMsg = inputText
                            viewModel.addChatMessage(userMsg, true)
                            inputText = ""
                            isTyping = true
                            scope.launch {
                                try {
                                    val userName = userInfo?.name ?: "estudante"
                                    val userCourse = userInfo?.course ?: "seu curso"
                                    val systemPrompt = "Você é o tutor do app Nexo. O usuário se chama $userName e estuda $userCourse. Responda de forma didática e motivadora. "
                                    
                                    var fullRes = ""
                                    assistant.askTutorStream(systemPrompt + userMsg).collect { chunk ->
                                        fullRes += chunk
                                    }
                                    viewModel.addChatMessage(fullRes, false)
                                } catch (e: Exception) {
                                    viewModel.addChatMessage("Erro ao conectar com a IA: ${e.localizedMessage}. Verifique sua conexão.", false)
                                } finally {
                                    isTyping = false
                                }
                            }
                        }
                    }, enabled = !isTyping) { Icon(Icons.AutoMirrored.Filled.Send, null, tint = MaterialTheme.colorScheme.primary) }
                }
            }
        }
    }
}

@Composable
fun MessageItem(message: ChatMessage, userInfo: UserInfo?) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        modifier = Modifier.fillMaxWidth()
    ) {
        ChatBubblePremium(message, userInfo)
    }
}

@Composable
fun SuggestionChipPremium(text: String, onClick: () -> Unit) {
    Surface(modifier = Modifier.clickable { onClick() }, shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) {
        Text(text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ChatBubblePremium(message: ChatMessage, userInfo: UserInfo?) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!message.isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(8.dp))
        }

        Surface(
            color = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.text,
                    color = if (message.isUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (!message.isUser && message.text != "...") {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(message.text))
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copiar",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, message.text)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, null)
                                context.startActivity(shareIntent)
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Compartilhar",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        if (message.isUser) {
            Spacer(Modifier.width(8.dp))
            if (userInfo?.profilePictureUri != null) {
                AsyncImage(
                    model = userInfo.profilePictureUri,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { index ->
            val infiniteTransition = rememberInfiniteTransition(label = "typing")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = index * 200),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot"
            )
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = alpha)))
        }
    }
}
