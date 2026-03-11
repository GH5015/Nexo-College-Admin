package com.example.collegeadmin.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.collegeadmin.ai.AiAssistant
import com.example.collegeadmin.LocalIndicatorClick
import com.example.collegeadmin.model.*
import com.example.collegeadmin.ui.CollegeViewModel
import com.example.collegeadmin.ui.components.ScreenIndicator
import com.example.collegeadmin.ui.components.EmptyStatePlaceholder
import com.example.collegeadmin.ui.components.HelpPopup
import com.example.collegeadmin.ui.components.HelpItem
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun SubjectsScreen(viewModel: CollegeViewModel, paddingValues: PaddingValues) {
    val subjects by viewModel.subjects.collectAsState()
    val userInfo by viewModel.userInfo.collectAsState()
    var selectedSubject by remember { mutableStateOf<Subject?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var subjectToEdit by remember { mutableStateOf<Subject?>(null) }
    val onMenuClick = LocalIndicatorClick.current

    var showHelp by remember { mutableStateOf(false) }

    LaunchedEffect(userInfo?.showHelp) {
        if (userInfo?.showHelp == true && selectedSubject == null) {
            showHelp = true
        }
    }

    if (showHelp) {
        HelpPopup(
            title = "Disciplinas",
            helpItems = listOf(
                HelpItem(
                    "Adicionar Matérias",
                    "Toque no botão '+' para cadastrar novas disciplinas, professores e salas.",
                    Icons.Default.AddCircle,
                    MaterialTheme.colorScheme.primary
                ),
                HelpItem(
                    "Frequência e Média",
                    "Cada card mostra seu progresso em tempo real. A barra muda de cor se você estiver com muitas faltas!",
                    Icons.Default.BarChart,
                    Color(0xFFF59E0B)
                ),
                HelpItem(
                    "Detalhes da Matéria",
                    "Toque em uma disciplina para gerenciar notas, faltas específicas e anotações de aula.",
                    Icons.Default.TouchApp,
                    Color(0xFF10B981)
                )
            ),
            onDismiss = { 
                showHelp = false
                viewModel.setShowHelp(false)
            }
        )
    }

    AnimatedContent(
        targetState = selectedSubject,
        transitionSpec = {
            fadeIn() + slideInHorizontally { it } togetherWith fadeOut() + slideOutHorizontally { -it }
        },
        label = "SubjectTransition"
    ) { subject ->
        if (subject == null) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(paddingValues)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item { 
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ScreenIndicator(
                                    label = "Disciplinas", 
                                    color = MaterialTheme.colorScheme.secondary,
                                    onClick = onMenuClick
                                )
                                Spacer(Modifier.width(8.dp))
                                IconButton(onClick = { showHelp = true }) {
                                    Icon(Icons.AutoMirrored.Filled.HelpOutline, "Ajuda", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
                                }
                            }
                            Text(
                                "Minhas Matérias", 
                                style = MaterialTheme.typography.headlineLarge, 
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                                fontWeight = FontWeight.ExtraBold
                            ) 
                        }
                    }
                    
                    if (subjects.isEmpty()) {
                        item {
                            EmptyStatePlaceholder("Nenhuma disciplina cadastrada. Toque no + para começar.")
                        }
                    } else {
                        items(subjects) { sub ->
                            SubjectCardPremium(
                                sub, 
                                onEdit = { subjectToEdit = sub },
                                onDelete = { viewModel.deleteSubject(sub.id) }
                            ) { selectedSubject = sub }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
                
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
                    shape = RoundedCornerShape(20.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, "Adicionar Disciplina")
                }
            }
        } else {
            val currentSub = subjects.find { it.id == subject.id }
            if (currentSub != null) {
                SubjectDetailScreen(currentSub, viewModel, onBack = { selectedSubject = null })
            } else {
                selectedSubject = null
            }
        }
    }

    if (showAddDialog) {
        SubjectDialog(
            userShift = userInfo?.shift ?: "Manhã",
            onDismiss = { showAddDialog = false }
        ) { name, prof, room, day, start, end ->
            viewModel.addSubject(name, prof, room, 40, day, start, end)
        }
    }

    if (subjectToEdit != null) {
        SubjectDialog(
            subject = subjectToEdit,
            userShift = userInfo?.shift ?: "Manhã",
            onDismiss = { subjectToEdit = null }
        ) { name, prof, room, _, _, _ ->
            viewModel.editSubject(subjectToEdit!!.id, name, prof, room, 40)
        }
    }
}

@Composable
fun SubjectDialog(
    subject: Subject? = null,
    userShift: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Int, LocalTime, LocalTime) -> Unit
) {
    var name by remember { mutableStateOf(subject?.name ?: "") }
    var prof by remember { mutableStateOf(subject?.professor ?: "") }
    var room by remember { mutableStateOf(subject?.room ?: "") }
    var selectedDay by remember { mutableIntStateOf(1) }
    
    val suggestedStart = when(userShift) {
        "Manhã" -> "08:00"
        "Tarde" -> "13:30"
        "Noite" -> "19:00"
        else -> "08:00"
    }
    val suggestedEnd = when(userShift) {
        "Manhã" -> "11:40"
        "Tarde" -> "17:10"
        "Noite" -> "22:30"
        else -> "11:40"
    }

    var startHour by remember { mutableStateOf(suggestedStart) }
    var endHour by remember { mutableStateOf(suggestedEnd) }
    
    val days = listOf("Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if(subject == null) "Nova Disciplina" else "Editar Disciplina") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome da Disciplina") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) }
                item { OutlinedTextField(value = prof, onValueChange = { prof = it }, label = { Text("Professor") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) }
                item { OutlinedTextField(value = room, onValueChange = { room = it }, label = { Text("Sala") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) }
                
                if (subject == null) {
                    item {
                        Text("Horário de Aula", style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                            items(days.size) { index ->
                                FilterChip(selected = selectedDay == index + 1, onClick = { selectedDay = index + 1 }, label = { Text(days[index]) })
                            }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = startHour, onValueChange = { startHour = it }, label = { Text("Início") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = endHour, onValueChange = { endHour = it }, label = { Text("Fim") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                        }
                    }
                }
            }
        },
        confirmButton = { 
            Button(onClick = { 
                if(name.isNotBlank()) {
                    try {
                        val start = LocalTime.parse(startHour)
                        val end = LocalTime.parse(endHour)
                        onConfirm(name, prof, room, selectedDay, start, end)
                        onDismiss()
                    } catch (e: Exception) { }
                }
            }) { Text(if(subject == null) "Adicionar" else "Salvar") } 
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun SubjectCardPremium(subject: Subject, onEdit: () -> Unit, onDelete: () -> Unit, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(subject.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(subject.professor, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) { 
                        Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp)) 
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) { 
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(18.dp)) 
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            val presence = (1.0f - (subject.absences.toFloat() / subject.totalClasses.toFloat())).coerceIn(0f, 1f)
            val progressColor = when {
                subject.isFailedByAbsence -> MaterialTheme.colorScheme.error
                subject.isAtRiskOfAbsence -> Color(0xFFF59E0B) 
                else -> MaterialTheme.colorScheme.secondary
            }
            
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Frequência", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text("${(presence * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { presence }, 
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                    color = progressColor,
                    trackColor = progressColor.copy(alpha = 0.1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Média: ${String.format(Locale.getDefault(), "%.1f", subject.averageGrade)}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.weight(1f))
                
                // Análise Preditiva Rápida no Card
                val p1 = subject.p1Grade
                if (p1 != null && subject.p2Grade == null) {
                    val neededFor7 = (14.0 - p1).coerceIn(0.0, 10.0)
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Precisa de ${String.format("%.1f", neededFor7)} na P2",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                } else if (p1 == null) {
                    Text("Sem notas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(subject: Subject, viewModel: CollegeViewModel, onBack: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Desempenho", "Faltas", "Agenda", "Conteúdo", "Info")
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(subject.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { p ->
        Column(modifier = Modifier.padding(p).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 20.dp,
                containerColor = Color.Transparent,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]).padding(horizontal = 16.dp).clip(CircleShape),
                        height = 4.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { 
                            Text(
                                title, 
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if(selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            ) 
                        }
                    )
                }
            }
            
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 24.dp)) {
                when(selectedTab) {
                    0 -> SubjectGradesTab(subject, viewModel)
                    1 -> AttendanceTab(subject, viewModel)
                    2 -> SubjectEventsTab(subject, viewModel)
                    3 -> SubjectNotesTab(subject, viewModel)
                    4 -> SubjectGeneralInfoTab(subject)
                }
            }
        }
    }
}

@Composable
fun AttendanceTab(subject: Subject, viewModel: CollegeViewModel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(40.dp))
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { (subject.absences.toFloat() / (subject.totalClasses * 0.25f)).coerceIn(0f, 1f) },
                modifier = Modifier.size(200.dp),
                strokeWidth = 16.dp,
                color = if(subject.isFailedByAbsence) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${subject.absences}", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                Text("Faltas", style = MaterialTheme.typography.labelLarge)
            }
        }
        
        Spacer(Modifier.height(48.dp))
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Limite de Faltas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Você pode ter até ${(subject.totalClasses * 0.25).toInt()} faltas neste semestre.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        
        Spacer(Modifier.weight(1f))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = { viewModel.addAbsence(subject.id) },
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Adicionar Falta")
            }
            if (subject.absences > 0) {
                OutlinedButton(
                    onClick = { viewModel.removeAbsence(subject.id) },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Remover")
                }
            }
        }
    }
}

@Composable
fun SubjectGeneralInfoTab(subject: Subject) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        DetailInfoCard(Icons.Default.Person, "Professor Responsável", subject.professor)
        DetailInfoCard(Icons.Default.MeetingRoom, "Sala de Aula", subject.room)
        DetailInfoCard(Icons.Default.Event, "Período Letivo", subject.period)
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (subject.passingProbability > 70) MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                                 else MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
            )
        ) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if(subject.passingProbability > 70) Icons.Default.CheckCircle else Icons.Default.Info, 
                    null, 
                    tint = if(subject.passingProbability > 70) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Status de Aprovação", style = MaterialTheme.typography.labelSmall)
                    Text(
                        "${String.format(Locale.getDefault(), "%.0f", subject.passingProbability)}% de chance de passar",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun DetailInfoCard(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SubjectNotesTab(subject: Subject, viewModel: CollegeViewModel) {
    val allNotes by viewModel.notes.collectAsState()
    val subjectNotes = allNotes.filter { it.subjectId == subject.id }
    var showAddNote by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val apiKey = "AIzaSyAQMyzTBaQ8zBy7J_-sRF84zpbuPhZHyNU"
    val assistant = remember { AiAssistant(apiKey) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        if (subjectNotes.isEmpty()) {
            EmptyStatePlaceholder("Nenhum conteúdo registrado para esta matéria.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(subjectNotes) { note ->
                    var aiResponse by remember { mutableStateOf<String?>(null) }
                    var isAiLoading by remember { mutableStateOf(false) }
                    
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(note.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text(note.date.format(DateTimeFormatter.ofPattern("dd MMM, yyyy")), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                                Row {
                                    IconButton(
                                        onClick = {
                                            if (apiKey.isNotEmpty()) {
                                                isAiLoading = true
                                                scope.launch {
                                                    aiResponse = assistant.askTutor("Explique melhor este tópico de aula: ${note.title}. Conteúdo: ${note.content}")
                                                    isAiLoading = false
                                                }
                                            }
                                        }
                                    ) {
                                        if (isAiLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        else Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(onClick = { viewModel.deleteNote(note.id) }) {
                                        Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            
                            Text(
                                text = note.content,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            if (note.attachments.isNotEmpty()) {
                                Spacer(Modifier.height(12.dp))
                                Text("Arquivos e Links", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    items(note.attachments) { attachment ->
                                        val isPdf = attachment.lowercase().endsWith(".pdf") || attachment.contains("document")
                                        
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.width(140.dp).clickable {
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(attachment))
                                                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    context.startActivity(intent)
                                                } catch (e: Exception) { }
                                            }
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        if (isPdf) Icons.Default.PictureAsPdf else Icons.AutoMirrored.Filled.InsertDriveFile,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = if (isPdf) Color.Red else MaterialTheme.colorScheme.primary
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(
                                                        if (isPdf) "Abrir PDF" else "Ver Arquivo",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                
                                                Spacer(Modifier.height(4.dp))
                                                
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End
                                                ) {
                                                    IconButton(
                                                        onClick = {
                                                            val sendIntent = Intent().apply {
                                                                action = Intent.ACTION_SEND
                                                                putExtra(Intent.EXTRA_STREAM, Uri.parse(attachment))
                                                                type = if (isPdf) "application/pdf" else "*/*"
                                                            }
                                                            val shareIntent = Intent.createChooser(sendIntent, "Salvar no Drive ou Compartilhar")
                                                            context.startActivity(shareIntent)
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.CloudUpload, 
                                                            "Drive", 
                                                            modifier = Modifier.size(14.dp),
                                                            tint = MaterialTheme.colorScheme.secondary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            if (aiResponse != null) {
                                Spacer(Modifier.height(16.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                        .padding(16.dp)
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                            Spacer(Modifier.width(6.6.dp))
                                            Text("Explicação da IA", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            Spacer(Modifier.weight(1f))
                                            IconButton(
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(aiResponse!!))
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        
                                        Text(
                                            text = aiResponse!!,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        
                                        TextButton(onClick = { aiResponse = null }, modifier = Modifier.align(Alignment.End)) {
                                            Text("Fechar", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }

        FloatingActionButton(
            onClick = { showAddNote = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Icon(Icons.AutoMirrored.Filled.NoteAdd, null)
        }
    }

    if (showAddNote) {
        var title by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }
        var difficulty by remember { mutableStateOf(Difficulty.MEDIUM) }
        val tempAttachments = remember { mutableStateListOf<String>() }
        
        val filePicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let { tempAttachments.add(it.toString()) }
        }

        AlertDialog(
            onDismissRequest = { showAddNote = false },
            title = { Text("Novo Conteúdo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = title, 
                        onValueChange = { title = it }, 
                        label = { Text("Título da Aula") }, 
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = content, 
                        onValueChange = { content = it }, 
                        label = { Text("O que foi visto?") }, 
                        modifier = Modifier.fillMaxWidth(), 
                        minLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Text("Dificuldade do conteúdo", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Difficulty.entries.forEach { diff ->
                            FilterChip(
                                selected = difficulty == diff,
                                onClick = { difficulty = diff },
                                label = { 
                                    Text(when(diff) {
                                        Difficulty.EASY -> "Fácil"
                                        Difficulty.MEDIUM -> "Médio"
                                        Difficulty.HARD -> "Difícil"
                                    }) 
                                }
                            )
                        }
                    }
                    
                    Text("Anexos (${tempAttachments.size})", style = MaterialTheme.typography.labelLarge)
                    
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(tempAttachments) { uri ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.AttachFile, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Arquivo", style = MaterialTheme.typography.labelSmall)
                                    IconButton(onClick = { tempAttachments.remove(uri) }, modifier = Modifier.size(20.dp)) {
                                        Icon(Icons.Default.Close, null, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                        item {
                            OutlinedIconButton(
                                onClick = { filePicker.launch("*/*") },
                                modifier = Modifier.size(32.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (title.isNotBlank()) {
                        viewModel.saveNote(null, subject.id, null, title, content, LocalDate.now(), tempAttachments.toList(), difficulty)
                        showAddNote = false
                    }
                }) { Text("Adicionar") }
            },
            dismissButton = {
                TextButton(onClick = { showAddNote = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun SubjectGradesTab(subject: Subject, viewModel: CollegeViewModel) {
    var p1 by remember { mutableStateOf(subject.p1Grade?.toString() ?: "") }
    var p2 by remember { mutableStateOf(subject.p2Grade?.toString() ?: "") }
    var pf by remember { mutableStateOf(subject.pfGrade?.toString() ?: "") }
    
    var p1Date by remember { mutableStateOf(subject.p1Date) }
    var p2Date by remember { mutableStateOf(subject.p2Date) }
    var pfDate by remember { mutableStateOf(subject.pfDate) }

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        // Bloco de Análise Preditiva
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Psychology, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text("Previsão de Aprovação", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))
                
                val p1Val = p1.toDoubleOrNull() ?: 0.0
                val p2Val = p2.toDoubleOrNull() ?: 0.0
                
                if (p1.isEmpty() && p2.isEmpty()) {
                    Text("Insira suas notas para calcular quanto você precisa para passar.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else if (p1.isNotEmpty() && p2.isEmpty()) {
                    val neededFor7 = (14.0 - p1Val).coerceIn(0.0, 10.0)
                    val neededFor5 = (10.0 - p1Val).coerceIn(0.0, 10.0)
                    
                    Text("Você precisa de ${String.format("%.1f", neededFor7)} na P2 para passar direto (média 7.0).", fontWeight = FontWeight.Bold)
                    Text("Mínimo de ${String.format("%.1f", neededFor5)} para ir para a Final (média 5.0).", style = MaterialTheme.typography.bodySmall)
                } else {
                    val currentAvg = (p1Val + p2Val) / 2
                    if (currentAvg >= 7.0) {
                        Text("Parabéns! Sua média atual é ${String.format("%.1f", currentAvg)}. Você está aprovado!", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                    } else if (currentAvg >= 5.0) {
                        val neededPF = (10.0 - currentAvg).coerceIn(0.0, 10.0)
                        Text("Média atual: ${String.format("%.1f", currentAvg)}. Você precisará de ${String.format("%.1f", neededPF)} na Prova Final.", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                    } else {
                        Text("Atenção! Sua média ${String.format("%.1f", currentAvg)} está abaixo do necessário para a final.", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        GradeInputCard(
            label = "Prova P1",
            grade = p1,
            date = p1Date,
            onGradeChange = { p1 = it },
            onDateChange = { p1Date = it }
        )
        
        GradeInputCard(
            label = "Prova P2",
            grade = p2,
            date = p2Date,
            onGradeChange = { p2 = it },
            onDateChange = { p2Date = it }
        )
        
        if (subject.needsPF) {
            GradeInputCard(
                label = "Prova Final (PF)",
                grade = pf,
                date = pfDate,
                onGradeChange = { pf = it },
                onDateChange = { pfDate = it }
            )
        }

        Button(
            onClick = { 
                viewModel.updateGrades(
                    subject.id, 
                    p1.toDoubleOrNull(), p1Date,
                    p2.toDoubleOrNull(), p2Date,
                    pf.toDoubleOrNull(), pfDate
                ) 
            }, 
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) { 
            Icon(Icons.Default.Save, null)
            Spacer(Modifier.width(8.dp))
            Text("Salvar Notas e Datas") 
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradeInputCard(
    label: String, 
    grade: String, 
    date: LocalDate?, 
    onGradeChange: (String) -> Unit, 
    onDateChange: (LocalDate?) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = date?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli() ?: System.currentTimeMillis()
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = grade, 
                    onValueChange = onGradeChange, 
                    label = { Text("Nota") }, 
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    placeholder = { Text("0.0") }
                )
                
                OutlinedTextField(
                    value = date?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "", 
                    onValueChange = {}, 
                    readOnly = true,
                    label = { Text("Data") }, 
                    placeholder = { Text("Agendar") },
                    modifier = Modifier.weight(1.5f).clickable { showDatePicker = true },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp),
                    trailingIcon = { 
                        IconButton(onClick = { showDatePicker = true }) { 
                            Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(18.dp)) 
                        } 
                    }
                )
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateChange(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate())
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectEventsTab(subject: Subject, viewModel: CollegeViewModel) {
    val assignments = subject.assignments
    var showAddAssignment by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (assignments.isEmpty()) {
            EmptyStatePlaceholder("Nenhum trabalho agendado para esta disciplina.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(assignments) { ass ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Assignment, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(ass.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = ass.date.format(DateTimeFormatter.ofPattern("dd 'de' MMMM", Locale.forLanguageTag("pt-BR"))),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { viewModel.deleteAssignment(ass.id) }) {
                                Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }

        FloatingActionButton(
            onClick = { showAddAssignment = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Icon(Icons.Default.Add, null)
        }
    }

    if (showAddAssignment) {
        var title by remember { mutableStateOf("") }
        var date by remember { mutableStateOf(LocalDate.now()) }
        var showDatePicker by remember { mutableStateOf(false) }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis()
        )

        AlertDialog(
            onDismissRequest = { showAddAssignment = false },
            title = { Text("Novo Trabalho / Atividade") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Título do Trabalho") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    OutlinedTextField(
                        value = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Data de Entrega") },
                        modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = { Icon(Icons.Default.CalendarToday, null) }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (title.isNotBlank()) {
                        viewModel.addAssignment(subject.id, title, date, EventType.ASSIGNMENT)
                        showAddAssignment = false
                    }
                }) { Text("Adicionar") }
            },
            dismissButton = {
                TextButton(onClick = { showAddAssignment = false }) { Text("Cancelar") }
            }
        )

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        }
                        showDatePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}
