package com.example.collegeadmin.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.collegeadmin.ai.AiAssistant
import com.example.collegeadmin.LocalIndicatorClick
import com.example.collegeadmin.calendar.CalendarAccount
import com.example.collegeadmin.model.*
import com.example.collegeadmin.ui.CollegeViewModel
import com.example.collegeadmin.ui.components.ScreenIndicator
import com.example.collegeadmin.ui.components.EmptyStatePlaceholder
import com.example.collegeadmin.ui.components.HelpPopup
import com.example.collegeadmin.ui.components.HelpItem
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

sealed class AiMindMapState {
    object Idle : AiMindMapState()
    object Loading : AiMindMapState()
    data class Success(val content: String) : AiMindMapState()
    data class Error(val msg: String) : AiMindMapState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: CollegeViewModel, paddingValues: PaddingValues) {
    val userInfo by viewModel.userInfo.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val events by viewModel.events.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val cr by viewModel.cr.collectAsState()
    val avgRetention by viewModel.avgRetention.collectAsState()
    val retentionHistory by viewModel.retentionHistory.collectAsState()
    val allPeriods by viewModel.allPeriods.collectAsState()
    val onMenuClick = LocalIndicatorClick.current
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    
    val apiKey = "AIzaSyAQMyzTBaQ8zBy7J_-sRF84zpbuPhZHyNU" 
    val assistant = remember { AiAssistant(apiKey) }
    
    var mindMapState by remember { mutableStateOf<AiMindMapState>(AiMindMapState.Idle) }
    var showFullMap by remember { mutableStateOf(false) }
    
    var showSyncDialog by remember { mutableStateOf(false) }
    var showUnsyncDialog by remember { mutableStateOf(false) }
    var availableCalendars by remember { mutableStateOf<List<CalendarAccount>>(emptyList()) }
    
    var showNewPeriodDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    
    var showHelp by remember { mutableStateOf(false) }

    LaunchedEffect(userInfo?.showHelp) {
        if (userInfo?.showHelp == true) {
            showHelp = true
        }
    }

    if (showHelp) {
        HelpPopup(
            title = "Seu Perfil",
            helpItems = listOf(
                HelpItem(
                    "Visão Geral",
                    "Acompanhe suas estatísticas de faltas e retenção de conteúdo. Edite sua foto e curso a qualquer momento.",
                    Icons.Default.Person,
                    MaterialTheme.colorScheme.primary
                ),
                HelpItem(
                    "Jornada Semanal",
                    "Navegue pela linha do tempo para ver seu progresso semana a semana durante o período letivo.",
                    Icons.Default.Timeline,
                    Color(0xFF10B981)
                ),
                HelpItem(
                    "Mapa Mental IA",
                    "Gere um mapa mental automático com tudo que você aprendeu na semana para visualizar e conectar ideias.",
                    Icons.Default.Psychology,
                    Color(0xFF8B5CF6)
                ),
                HelpItem(
                    "Sincronia com Agenda",
                    "Sincronize ou remova seus eventos acadêmicos com o Google Agenda para não perder nenhum prazo.",
                    Icons.Default.Sync,
                    Color(0xFFF59E0B)
                )
            ),
            onDismiss = { 
                showHelp = false 
                viewModel.setShowHelp(false)
            }
        )
    }

    val today = LocalDate.now()
    val startOfWeek = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
    val weeklyNotes = notes.filter { !it.date.isBefore(startOfWeek) }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            availableCalendars = viewModel.getAvailableCalendars(context)
            if (availableCalendars.isNotEmpty()) {
                if (!showUnsyncDialog) showSyncDialog = true else showUnsyncDialog = true
            }
        } else {
            Toast.makeText(context, "Permissões de calendário são necessárias.", Toast.LENGTH_SHORT).show()
        }
    }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { 
            try { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: Exception) { }
            viewModel.updateProfilePicture(it.toString()) 
        }
    }
    
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 20.dp), 
            horizontalAlignment = Alignment.CenterHorizontally, 
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ScreenIndicator(
                            label = "Perfil", 
                            color = MaterialTheme.colorScheme.primary,
                            onClick = onMenuClick
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { showHelp = true }) {
                            Icon(Icons.AutoMirrored.Filled.HelpOutline, "Ajuda", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    
                    val primaryBrush = Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
                    
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Surface(
                            modifier = Modifier.size(130.dp).padding(4.dp).clickable { photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            shape = CircleShape,
                            border = BorderStroke(3.dp, primaryBrush),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            if (userInfo?.profilePictureUri != null) {
                                AsyncImage(model = userInfo?.profilePictureUri, contentDescription = "Perfil", modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                            } else {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, modifier = Modifier.size(70.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) }
                            }
                        }
                        Surface(modifier = Modifier.size(36.dp), shape = CircleShape, color = MaterialTheme.colorScheme.secondary, shadowElevation = 6.dp) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp), tint = Color.White) }
                        }
                    }
                    
                    Spacer(Modifier.height(20.dp))
                    Text(text = userInfo?.name ?: "Futuro Graduado", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    
                    Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), shape = RoundedCornerShape(30.dp)) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.School, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(text = userInfo?.course ?: "Curso não definido", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Card Principal de CR
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Coeficiente de Rendimento (CR)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                            Text(
                                text = String.format(Locale.getDefault(), "%.2f", cr),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black,
                                color = when {
                                    cr >= 7.0 -> Color(0xFF10B981)
                                    cr >= 5.0 -> Color(0xFFF59E0B)
                                    else -> Color(0xFFEF4444)
                                }
                            )
                        }
                        Box(
                            modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Analytics, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val avgAbs = if(subjects.isNotEmpty()) subjects.sumOf { it.absences }.toDouble() / subjects.size else 0.0
                    ProfileStatCardPremium(modifier = Modifier.weight(1f), label = "Média Faltas", value = String.format(Locale.getDefault(), "%.1f", avgAbs), icon = Icons.Default.Warning, color = if(avgAbs > 5) Color(0xFFEF4444) else Color(0xFFF59E0B))
                    
                    Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Psychology, null, tint = Color(0xFF10B981), modifier = Modifier.size(24.dp))
                                SparklineChart(data = retentionHistory, color = Color(0xFF10B981), modifier = Modifier.size(40.dp, 24.dp))
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("${avgRetention.toInt()}%", fontSize = 28.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                            Text("Nível Retenção", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SettingsSuggest, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Gestão Acadêmica", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ManagementItem(
                                modifier = Modifier.weight(1f),
                                label = "Novo Período",
                                icon = Icons.Default.AddCircle,
                                color = MaterialTheme.colorScheme.primary,
                                onClick = { showNewPeriodDialog = true }
                            )
                            ManagementItem(
                                modifier = Modifier.weight(1f),
                                label = "Histórico",
                                icon = Icons.Default.History,
                                color = MaterialTheme.colorScheme.tertiary,
                                onClick = { showHistoryDialog = true }
                            )
                        }
                    }
                }
            }

            item { AcademicTimelineWrapped(events, notes, userInfo) }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(), 
                    shape = RoundedCornerShape(32.dp), 
                    color = MaterialTheme.colorScheme.surface, 
                    tonalElevation = 3.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), 
                                contentAlignment = Alignment.Center
                            ) { 
                                Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) 
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text("Mapa Mental da Semana", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Sintetize seus estudos com IA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                        
                        Spacer(Modifier.height(24.dp))
                        
                        when (val state = mindMapState) {
                            is AiMindMapState.Idle -> {
                                Text(
                                    "A IA analisará todas as suas anotações desta semana para criar uma estrutura lógica e visual do que você aprendeu.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 20.dp)
                                )
                                Button(
                                    onClick = { 
                                        mindMapState = AiMindMapState.Loading
                                        scope.launch { 
                                            try {
                                                val res = assistant.generateMindMap(weeklyNotes.joinToString("\n") { "${it.title}: ${it.content}" })
                                                mindMapState = AiMindMapState.Success(res)
                                            } catch (e: Exception) {
                                                mindMapState = AiMindMapState.Error("Não foi possível gerar o mapa. Verifique sua conexão.")
                                            }
                                        } 
                                    }, 
                                    enabled = weeklyNotes.isNotEmpty(), 
                                    modifier = Modifier.fillMaxWidth().height(54.dp), 
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(Icons.Default.Psychology, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Gerar Mapa Mental ✨")
                                }
                                if (weeklyNotes.isEmpty()) {
                                    Text(
                                        "Adicione anotações nas aulas desta semana para habilitar.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            is AiMindMapState.Loading -> {
                                Column(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(modifier = Modifier.size(32.dp), color = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.height(12.dp))
                                    Text("Nexo está processando suas anotações...", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            is AiMindMapState.Error -> {
                                Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(state.msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                    TextButton(onClick = { mindMapState = AiMindMapState.Idle }) { Text("Tentar novamente") }
                                }
                            }
                            is AiMindMapState.Success -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        .clickable { showFullMap = true }
                                        .padding(16.dp)
                                ) {
                                    Column {
                                        MindMapHierarchyView(
                                            text = state.content,
                                            maxLines = 8,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text("Toque para ver tudo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                            Row {
                                                IconButton(onClick = { 
                                                    clipboardManager.setText(AnnotatedString(state.content))
                                                    Toast.makeText(context, "Mapa Mental copiado!", Toast.LENGTH_SHORT).show()
                                                }) {
                                                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                                }
                                                IconButton(onClick = { mindMapState = AiMindMapState.Idle }) {
                                                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Google Agenda", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                    
                    Button(
                        onClick = { 
                            showUnsyncDialog = false
                            checkAndRequestCalendarPermission(context, {
                                availableCalendars = viewModel.getAvailableCalendars(context)
                                if (availableCalendars.isNotEmpty()) showSyncDialog = true
                            }, calendarPermissionLauncher)
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Sync, null)
                        Spacer(Modifier.width(12.dp))
                        Text("Sincronizar Agora", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { 
                            showUnsyncDialog = true
                            checkAndRequestCalendarPermission(context, {
                                availableCalendars = viewModel.getAvailableCalendars(context)
                                if (availableCalendars.isNotEmpty()) showUnsyncDialog = true
                            }, calendarPermissionLauncher)
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.SyncDisabled, null)
                        Spacer(Modifier.width(12.dp))
                        Text("Dessincronizar Eventos", fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }

    if (showFullMap && mindMapState is AiMindMapState.Success) {
        val content = (mindMapState as AiMindMapState.Success).content
        Dialog(
            onDismissRequest = { showFullMap = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { showFullMap = false }) {
                            Icon(Icons.Default.Close, null)
                        }
                        Text("Mapa Mental Estruturado", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Row {
                            IconButton(onClick = { exportMindMapPdf(context, content) }) {
                                Icon(Icons.Default.PictureAsPdf, null, tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { 
                                clipboardManager.setText(AnnotatedString(content))
                                Toast.makeText(context, "Texto copiado!", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.ContentCopy, null)
                            }
                        }
                    }
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    
                    SelectionContainer {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(24.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(24.dp)) {
                                    MindMapHierarchyView(text = content)
                                }
                            }
                            Spacer(Modifier.height(48.dp))
                        }
                    }
                }
            }
        }
    }

    if (showSyncDialog) {
        AlertDialog(
            onDismissRequest = { showSyncDialog = false },
            title = { Text("Escolha a Agenda") },
            text = {
                Column {
                    Text("Selecione qual conta do Google deseja sincronizar os compromissos acadêmicos:")
                    Spacer(Modifier.height(16.dp))
                    availableCalendars.forEach { cal ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable {
                                viewModel.syncToCalendar(context, cal.id, true)
                                showSyncDialog = false
                            }.padding(vertical = 8.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(cal.name, fontWeight = FontWeight.Bold)
                                Text(cal.email, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSyncDialog = false }) { Text("Fechar") } }
        )
    }

    if (showUnsyncDialog) {
        AlertDialog(
            onDismissRequest = { showUnsyncDialog = false },
            title = { Text("Remover da Agenda") },
            text = {
                Column {
                    Text("Escolha de qual conta deseja remover os eventos do College Admin:")
                    Spacer(Modifier.height(16.dp))
                    availableCalendars.forEach { cal ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable {
                                viewModel.unsyncCalendar(context, cal.id)
                                showUnsyncDialog = false
                            }.padding(vertical = 8.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(cal.name, fontWeight = FontWeight.Bold)
                                Text(cal.email, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showUnsyncDialog = false }) { Text("Cancelar") } }
        )
    }

    if (showNewPeriodDialog) {
        AlertDialog(onDismissRequest = { showNewPeriodDialog = false }, confirmButton = { Button(onClick = { showNewPeriodDialog = false }) { Text("Entendido") } }, title = { Text("Novo Semestre") }, text = { Text("Esta função prepara seu app para o próximo desafio acadêmico.") })
    }
    
    if (showHistoryDialog) {
        ModalBottomSheet(
            onDismissRequest = { showHistoryDialog = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
            containerColor = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Histórico Geral", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Surface(
                        color = when {
                            cr >= 7.0 -> Color(0xFF10B981)
                            cr >= 5.0 -> Color(0xFFF59E0B)
                            else -> Color(0xFFEF4444)
                        }.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "CR: ${String.format(Locale.getDefault(), "%.2f", cr)}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                cr >= 7.0 -> Color(0xFF10B981)
                                cr >= 5.0 -> Color(0xFFF59E0B)
                                else -> Color(0xFFEF4444)
                            }
                        )
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                
                if (allPeriods.isEmpty()) {
                    EmptyStatePlaceholder("Nenhum período registrado no histórico.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        allPeriods.forEach { period ->
                            item {
                                Text(period, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(8.dp))
                                val periodSubjects = subjects.filter { it.period == period }
                                periodSubjects.forEach { sub ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(sub.name, fontWeight = FontWeight.Bold)
                                                Text(sub.professor, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = String.format(Locale.getDefault(), "%.1f", sub.averageGrade),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Black,
                                                    color = if(sub.averageGrade >= 7.0) Color(0xFF10B981) else if(sub.averageGrade >= 5.0) Color(0xFFF59E0B) else Color(0xFFEF4444)
                                                )
                                                Text("Média Final", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

fun checkAndRequestCalendarPermission(
    context: Context,
    onPermissionGranted: () -> Unit,
    launcher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>
) {
    val permissions = arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
    val allGranted = permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    if (allGranted) {
        onPermissionGranted()
    } else {
        val activity = context as? Activity
        val shouldShowRationale = permissions.any { 
            activity?.let { act -> ActivityCompat.shouldShowRequestPermissionRationale(act, it) } ?: false
        }

        if (shouldShowRationale) {
            Toast.makeText(context, "Precisamos de acesso ao calendário para sincronizar seus compromissos.", Toast.LENGTH_LONG).show()
            launcher.launch(permissions)
        } else {
            // Pode estar negado permanentemente ou ser a primeira vez
            launcher.launch(permissions)
        }
    }
}

@Composable
fun ManagementItem(modifier: Modifier, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun SparklineChart(data: List<Double>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (data.size < 2) return@Canvas
        
        val path = Path()
        val width = size.width
        val height = size.height
        val max = data.maxOrNull()?.toFloat()?.coerceAtLeast(100f) ?: 100f
        val min = 0f
        
        val xStep = width / (data.size - 1)
        
        data.forEachIndexed { index, value ->
            val x = index * xStep
            val y = height - ((value.toFloat() - min) / (max - min) * height)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
fun MindMapHierarchyView(text: String, maxLines: Int = Int.MAX_VALUE, overflow: TextOverflow = TextOverflow.Clip) {
    val lines = text.lines().filter { it.isNotBlank() }.let { 
        if (maxLines != Int.MAX_VALUE && it.size > maxLines) it.take(maxLines) else it
    }
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        lines.forEach { line ->
            val indentLevel = when {
                line.contains("🧠") -> 0
                line.contains("📁") -> 1
                line.contains("🌿") -> 2
                line.contains("🔹") -> 3
                line.contains("💡") -> 3
                else -> 0
            }
            
            Row(modifier = Modifier.padding(start = (indentLevel * 16).dp)) {
                Text(
                    text = line.trim(),
                    style = when(indentLevel) {
                        0 -> MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black)
                        1 -> MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        else -> MaterialTheme.typography.bodySmall
                    },
                    color = when(indentLevel) {
                        0 -> MaterialTheme.colorScheme.primary
                        1 -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = overflow
                )
            }
        }
    }
}

@Composable
fun ProfileStatCardPremium(modifier: Modifier, label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Surface(modifier = modifier, shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
        Column(modifier = Modifier.padding(20.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(12.dp))
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun ActionButtonPremium(modifier: Modifier, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    Surface(modifier = modifier.height(90.dp).clickable { onClick() }, shape = RoundedCornerShape(24.dp), color = color.copy(alpha = 0.1f), border = BorderStroke(1.dp, color.copy(alpha = 0.2f))) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color)
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun AcademicTimelineWrapped(events: List<AcademicEvent>, notes: List<ClassNote>, userInfo: UserInfo?) {
    if (userInfo == null) return

    val today = LocalDate.now()
    val totalWeeks = ChronoUnit.WEEKS.between(userInfo.periodStart, userInfo.periodEnd).toInt().coerceAtLeast(1)
    val currentWeek = ChronoUnit.WEEKS.between(userInfo.periodStart, today).toInt().coerceIn(0, totalWeeks - 1)

    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Status da Jornada", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(8.dp)) {
                Text(
                    "Semana ${currentWeek + 1} de $totalWeeks", 
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(end = 20.dp)
        ) {
            items(totalWeeks) { weekIdx ->
                val weekStart = userInfo.periodStart.plusWeeks(weekIdx.toLong())
                val weekEnd = weekStart.plusDays(6)
                
                val weekNotes = notes.count { !it.date.isBefore(weekStart) && !it.date.isAfter(weekEnd) }
                val weekEvents = events.count { !it.date.isBefore(weekStart) && !it.date.isAfter(weekEnd) }
                
                val isCurrent = weekIdx == currentWeek
                val isPast = weekIdx < currentWeek
                
                val cardColor = when {
                    isCurrent -> MaterialTheme.colorScheme.primaryContainer
                    isPast -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.surface
                }

                val borderColor = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Transparent

                Card(
                    modifier = Modifier.width(180.dp).height(140.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    border = if (isCurrent) BorderStroke(2.dp, borderColor) else null,
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 4.dp else 1.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "SEMANA ${weekIdx + 1}", 
                                style = MaterialTheme.typography.labelMedium, 
                                fontWeight = FontWeight.Black, 
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isPast) {
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                            }
                        }
                        
                        Text(
                            "${weekStart.format(DateTimeFormatter.ofPattern("dd/MM"))} - ${weekEnd.format(DateTimeFormatter.ofPattern("dd/MM"))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        
                        Spacer(Modifier.weight(1f))
                        
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.NoteAdd, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(6.6.dp))
                                Text("$weekNotes anotações", style = MaterialTheme.typography.bodySmall)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Event, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.width(6.6.dp))
                                Text("$weekEvents compromissos", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun exportMindMapPdf(context: Context, content: String) {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
    val page = pdfDocument.startPage(pageInfo)
    val canvas: Canvas = page.canvas
    val paint = Paint()

    // Header Background
    paint.color = android.graphics.Color.rgb(139, 92, 246) // Purple 600
    canvas.drawRect(0f, 0f, 595f, 120f, paint)

    // NEXO Logo
    paint.color = android.graphics.Color.WHITE
    paint.textSize = 28f
    paint.isFakeBoldText = true
    canvas.drawText("NEXO AI", 40f, 60f, paint)

    paint.textSize = 12f
    paint.isFakeBoldText = false
    canvas.drawText("Mapa Mental Estruturado da Semana", 40f, 85f, paint)

    // Body
    paint.color = android.graphics.Color.BLACK
    paint.textSize = 12f
    
    val margin = 40f
    var yPos = 160f
    val lineSpacing = 22f

    content.lines().forEach { line ->
        if (line.isBlank()) return@forEach
        
        val indentLevel = when {
            line.contains("🧠") -> 0
            line.contains("📁") -> 1
            line.contains("🌿") -> 2
            line.contains("🔹") -> 3
            line.contains("💡") -> 3
            else -> 0
        }

        paint.isFakeBoldText = indentLevel <= 1
        paint.textSize = when(indentLevel) {
            0 -> 18f
            1 -> 14f
            else -> 12f
        }
        
        val xPos = margin + (indentLevel * 20f)
        canvas.drawText(line.trim(), xPos, yPos, paint)
        yPos += lineSpacing

        if (yPos > 800f) return@forEach // Basic overflow protection
    }

    // Footer
    paint.color = android.graphics.Color.GRAY
    paint.textSize = 10f
    paint.isFakeBoldText = false
    canvas.drawText("Gerado automaticamente pelo College Admin - Nexo", margin, 820f, paint)

    pdfDocument.finishPage(page)

    val fileName = "Mapa_Mental_Nexo_${LocalDate.now()}.pdf"
    val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

    try {
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(shareIntent, "Compartilhar Mapa Mental..."))

    } catch (e: Exception) {
        Toast.makeText(context, "Erro ao gerar PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        pdfDocument.close()
    }
}
