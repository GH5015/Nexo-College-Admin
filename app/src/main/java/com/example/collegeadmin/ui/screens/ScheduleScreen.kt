package com.example.collegeadmin.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.example.collegeadmin.LocalIndicatorClick
import com.example.collegeadmin.model.ClassSession
import com.example.collegeadmin.ui.CollegeViewModel
import com.example.collegeadmin.ui.CalendarDay
import com.example.collegeadmin.ui.components.ScreenIndicator
import com.example.collegeadmin.ui.components.EmptyStatePlaceholder
import com.example.collegeadmin.ui.components.HelpPopup
import com.example.collegeadmin.ui.components.HelpItem
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun ScheduleScreen(viewModel: CollegeViewModel, paddingValues: PaddingValues) {
    val selectedSession by viewModel.selectedSession.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    
    AnimatedContent(
        targetState = selectedSession,
        transitionSpec = {
            fadeIn() + slideInVertically { it } togetherWith fadeOut() + slideOutVertically { -it }
        },
        label = "ScheduleTransition"
    ) { session ->
        if (session == null) {
            ScheduleList(viewModel, paddingValues) { s, d -> 
                viewModel.setSelectedSession(s, d) 
            }
        } else {
            ClassNotesScreen(
                session = session, 
                date = selectedDate ?: LocalDate.now(), 
                viewModel = viewModel, 
                onBack = { viewModel.setSelectedSession(null) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleList(viewModel: CollegeViewModel, paddingValues: PaddingValues, onSessionClick: (ClassSession, LocalDate) -> Unit) {
    val userInfo by viewModel.userInfo.collectAsState()
    val fullSchedule by viewModel.fullSchedule.collectAsState()
    val currentWeekSchedule by viewModel.currentWeekSchedule.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedSubjectId by remember { mutableStateOf<String?>(null) }
    val onMenuClick = LocalIndicatorClick.current

    val filteredCurrentWeek = remember(currentWeekSchedule, selectedSubjectId) {
        if (selectedSubjectId == null) currentWeekSchedule
        else currentWeekSchedule.map { day ->
            day.copy(sessions = day.sessions.filter { it.subjectId == selectedSubjectId })
        }.filter { it.sessions.isNotEmpty() }
    }

    val filteredFullSchedule = remember(fullSchedule, selectedSubjectId) {
        if (selectedSubjectId == null) fullSchedule
        else fullSchedule.mapValues { (_, days) ->
            days.map { day ->
                day.copy(sessions = day.sessions.filter { it.subjectId == selectedSubjectId })
            }.filter { it.sessions.isNotEmpty() }
        }.filterValues { it.isNotEmpty() }
    }

    var showHelp by remember { mutableStateOf(false) }

    LaunchedEffect(userInfo?.showHelp) {
        if (userInfo?.showHelp == true) {
            showHelp = true
        }
    }

    if (showHelp) {
        HelpPopup(
            title = "Cronograma de Aulas",
            helpItems = listOf(
                HelpItem(
                    "Visualização Flexível",
                    "Alterne entre Hoje, Semana e Período para ver suas aulas passadas e futuras.",
                    Icons.Default.DateRange,
                    MaterialTheme.colorScheme.primary
                ),
                HelpItem(
                    "Filtro por Disciplina",
                    "Use os botões de filtro no topo para isolar as aulas de uma matéria específica.",
                    Icons.Default.FilterList,
                    MaterialTheme.colorScheme.secondary
                ),
                HelpItem(
                    "Registro de Conteúdo",
                    "Toque em uma aula para escrever o que aprendeu, anexar fotos do quadro ou arquivos em PDF.",
                    Icons.Default.HistoryEdu,
                    Color(0xFF10B981)
                )
            ),
            onDismiss = { showHelp = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(paddingValues)) {
        Box(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ScreenIndicator(
                    label = "Aulas", 
                    color = MaterialTheme.colorScheme.primary,
                    onClick = onMenuClick
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { showHelp = true }) {
                    Icon(Icons.AutoMirrored.Filled.HelpOutline, "Ajuda", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
                }
            }
        }
        
        Text(
            "Cronograma", 
            style = MaterialTheme.typography.headlineLarge, 
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            fontWeight = FontWeight.Black
        )

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
            listOf("Hoje", "Semana", "Período").forEachIndexed { index, title ->
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

        // Filtro de Matérias
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedSubjectId == null,
                    onClick = { selectedSubjectId = null },
                    label = { Text("Todas") },
                    leadingIcon = if (selectedSubjectId == null) {
                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
            items(subjects) { subject ->
                FilterChip(
                    selected = selectedSubjectId == subject.id,
                    onClick = { selectedSubjectId = subject.id },
                    label = { Text(subject.name) },
                    leadingIcon = if (selectedSubjectId == subject.id) {
                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
        }

        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
                } else {
                    (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(slideOutHorizontally { width -> width } + fadeOut())
                }
            },
            label = "TabContentAnimation",
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)
        ) { targetTabIndex ->
            when(targetTabIndex) {
                0 -> TodayTab(filteredCurrentWeek, onSessionClick)
                1 -> WeeklyTab(filteredCurrentWeek, onSessionClick)
                2 -> PeriodTab(filteredFullSchedule, onSessionClick)
            }
        }
    }
}

@Composable
fun TodayTab(currentWeekDays: List<CalendarDay>, onSessionClick: (ClassSession, LocalDate) -> Unit) {
    val today = LocalDate.now()
    val todaySessions = currentWeekDays.find { it.date == today }?.sessions ?: emptyList()
    
    if (todaySessions.isEmpty()) {
        EmptyStatePlaceholder("Nenhuma aula encontrada para hoje.")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(todaySessions) { s -> SessionCardPremium(session = s, date = today, onClick = onSessionClick) }
        }
    }
}

@Composable
fun WeeklyTab(currentWeekDays: List<CalendarDay>, onSessionClick: (ClassSession, LocalDate) -> Unit) {
    val dayNames = listOf("Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo")
    
    if (currentWeekDays.isEmpty()) {
        EmptyStatePlaceholder("Sem aulas para esta seleção nesta semana.")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            items(currentWeekDays) { day ->
                Column {
                    Text(
                        dayNames[day.date.dayOfWeek.value - 1], 
                        style = MaterialTheme.typography.titleMedium, 
                        color = MaterialTheme.colorScheme.primary, 
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))
                    day.sessions.forEach { s ->
                        SessionCardPremium(session = s, date = day.date, onClick = onSessionClick, isPast = day.isPast)
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PeriodTab(fullSchedule: Map<Int, List<CalendarDay>>, onSessionClick: (ClassSession, LocalDate) -> Unit) {
    if (fullSchedule.isEmpty()) {
        EmptyStatePlaceholder("Nenhuma aula encontrada para o período selecionado.")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            fullSchedule.forEach { (weekNum, days) ->
                item {
                    Column {
                        Text(
                            "Semana $weekNum",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.height(16.dp))
                        days.forEach { day ->
                            day.sessions.forEach { session ->
                                SessionCardPremium(
                                    session = session,
                                    date = day.date,
                                    onClick = onSessionClick,
                                    dateLabel = day.date.format(DateTimeFormatter.ofPattern("dd/MM")),
                                    isPast = day.isPast
                                )
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SessionCardPremium(
    session: ClassSession, 
    date: LocalDate,
    onClick: (ClassSession, LocalDate) -> Unit, 
    modifier: Modifier = Modifier,
    dateLabel: String? = null,
    isPast: Boolean = false
) {
    val formattedStart = remember(session.startTime) { session.startTime.toString() }
    val formattedEnd = remember(session.endTime) { session.endTime.toString() }
    
    val now = LocalDate.now()
    val currentTime = LocalTime.now()
    val isToday = date == now
    val isLive = isToday && !currentTime.isBefore(session.startTime) && currentTime.isBefore(session.endTime)
    val hasEnded = isPast || (isToday && !currentTime.isBefore(session.endTime))

    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(if (isLive) pulseScale else 1f)
            .clickable { onClick(session, date) },
        shape = RoundedCornerShape(24.dp),
        color = when {
            isLive -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
            hasEnded -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            else -> MaterialTheme.colorScheme.surface
        },
        border = if (isLive) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        tonalElevation = if (hasEnded) 0.dp else 2.dp
    ) {
        Row(
            modifier = Modifier.padding(20.dp), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp)) {
                if(dateLabel != null) {
                    Text(dateLabel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if(isLive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary)
                }
                Text(formattedStart, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(formattedEnd, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            
            Spacer(Modifier.width(20.dp))
            
            Box(modifier = Modifier.width(1.dp).height(40.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)))
            
            Spacer(Modifier.width(20.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                if (isLive) {
                    Surface(
                        color = Color(0xFFEF4444),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Text(
                            "AO VIVO", 
                            color = Color.White, 
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                Text(
                    session.subjectName, 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold,
                    color = if (hasEnded) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MeetingRoom, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.width(4.dp))
                    Text(session.room, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
                
                if (isLive) {
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { onClick(session, date) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Mic, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.6.dp))
                        Text("Transcrição Ativa", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            if (hasEnded) {
                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
            } else {
                Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassNotesScreen(
    session: ClassSession, 
    date: LocalDate, 
    viewModel: CollegeViewModel, 
    onBack: () -> Unit
) {
    val allNotes by viewModel.notes.collectAsState()
    val note = allNotes.find { it.sessionId == session.id && it.date == date }
    var showEditNote by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val newAttachments = uris.map { it.toString() }
            viewModel.saveNote(
                id = note?.id,
                subjectId = session.subjectId, 
                sessionId = session.id,
                title = note?.title ?: "Anotação de ${session.subjectName}", 
                content = note?.content ?: "", 
                date = date,
                attachments = (note?.attachments ?: emptyList()) + newAttachments
            )
        }
    }

    Scaffold(
        topBar = { 
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(session.subjectName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), style = MaterialTheme.typography.labelSmall)
                    }
                }, 
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    if (note != null) {
                        IconButton(onClick = { exportNotePdf(context, session.subjectName, date, note.title, note.content) }) {
                            Icon(Icons.Default.PictureAsPdf, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            ) 
        },
        floatingActionButton = { 
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallFloatingActionButton(
                    onClick = { filePickerLauncher.launch("*/*") }, 
                    containerColor = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AttachFile, null, tint = MaterialTheme.colorScheme.primary)
                }
                ExtendedFloatingActionButton(
                    onClick = { showEditNote = true },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    icon = { Icon(if (note == null) Icons.AutoMirrored.Filled.NoteAdd else Icons.Default.Edit, null) },
                    text = { Text(if (note == null) "Criar Anotação" else "Editar") }
                )
            }
        }
    ) { p ->
        Column(modifier = Modifier.padding(p).fillMaxSize().background(MaterialTheme.colorScheme.background).padding(20.dp)) {
            if (note == null) {
                EmptyStatePlaceholder("Nenhuma anotação vinculada a esta aula nesta data específica.")
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                        Text(note.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "Aula de ${date.format(DateTimeFormatter.ofPattern("dd MMMM, yyyy"))}", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(Modifier.height(24.dp))
                        
                        Text(
                            text = note.content, 
                            style = MaterialTheme.typography.bodyLarge, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        if (note.attachments.isNotEmpty()) {
                            Spacer(Modifier.height(32.dp))
                            Text("Arquivos e Mídia", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(16.dp))
                            
                            // Carrossel de Anexos
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 12.dp)
                            ) {
                                items(note.attachments) { fileUri ->
                                    AttachmentPreviewCard(fileUri, context)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditNote) {
        var title by remember { mutableStateOf(note?.title ?: "") }
        var content by remember { mutableStateOf(note?.content ?: "") }

        AlertDialog(
            onDismissRequest = { showEditNote = false },
            title = { Text("Bloco de Notas") },
            text = { 
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = title, 
                        onValueChange = { title = it }, 
                        label = { Text("Título") }, 
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = content, 
                        onValueChange = { content = it }, 
                        label = { Text("Conteúdo") }, 
                        modifier = Modifier.fillMaxWidth(), 
                        minLines = 5,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = { 
                Button(onClick = { 
                    viewModel.saveNote(note?.id, session.subjectId, session.id, title, content, date, note?.attachments ?: emptyList())
                    showEditNote = false 
                }) { Text("Salvar") } 
            }
        )
    }
}

@Composable
fun AttachmentPreviewCard(fileUri: String, context: Context) {
    val uri = fileUri.toUri()
    val extension = remember(fileUri) { 
        fileUri.substringAfterLast('.', "").lowercase() 
    }
    val isImage = extension in listOf("jpg", "jpeg", "png", "webp", "gif")
    
    Surface(
        modifier = Modifier
            .size(width = 140.dp, height = 100.dp)
            .clickable { 
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = uri
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Erro ao abrir arquivo", Toast.LENGTH_SHORT).show()
                }
            },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        if (isImage) {
            AsyncImage(
                model = fileUri,
                contentDescription = "Preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = when(extension) {
                        "pdf" -> Icons.Default.PictureAsPdf
                        "doc", "docx" -> Icons.Default.Description
                        else -> Icons.AutoMirrored.Filled.InsertDriveFile
                    },
                    contentDescription = null,
                    tint = when(extension) {
                        "pdf" -> Color(0xFFEF4444)
                        "doc", "docx" -> Color(0xFF3B82F6)
                        else -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = uri.lastPathSegment ?: "Arquivo",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

private fun exportNotePdf(context: Context, subjectName: String, date: LocalDate, title: String, content: String) {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
    val page = pdfDocument.startPage(pageInfo)
    val canvas: Canvas = page.canvas
    val paint = Paint()

    // Header Background
    paint.color = android.graphics.Color.rgb(99, 102, 241) // Primary Indigo
    canvas.drawRect(0f, 0f, 595f, 120f, paint)

    // Logo / Title App
    paint.color = android.graphics.Color.WHITE
    paint.textSize = 24f
    paint.isFakeBoldText = true
    canvas.drawText("Nexo", 40f, 50f, paint)

    paint.textSize = 12f
    paint.isFakeBoldText = false
    canvas.drawText("Seu assistente acadêmico inteligente", 40f, 70f, paint)

    // Subject and Date info in header
    paint.textAlign = Paint.Align.RIGHT
    paint.textSize = 14f
    paint.isFakeBoldText = true
    canvas.drawText(subjectName, 555f, 50f, paint)
    
    paint.isFakeBoldText = false
    paint.textSize = 12f
    canvas.drawText(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), 555f, 70f, paint)

    // Content Title
    paint.textAlign = Paint.Align.LEFT
    paint.color = android.graphics.Color.BLACK
    paint.textSize = 20f
    paint.isFakeBoldText = true
    canvas.drawText(title, 40f, 160f, paint)

    // Divider line
    paint.color = android.graphics.Color.LTGRAY
    canvas.drawLine(40f, 175f, 555f, 175f, paint)

    // Content Body
    paint.color = android.graphics.Color.DKGRAY
    paint.textSize = 13f
    paint.isFakeBoldText = false
    
    val margin = 40f
    val maxWidth = 515f
    var yPos = 210f
    
    // Simple text wrapping logic
    val words = content.split(" ", "\n")
    var line = StringBuilder()
    
    for (word in words) {
        val testLine = if (line.isEmpty()) word else "$line $word"
        val textWidth = paint.measureText(testLine)
        
        if (textWidth > maxWidth) {
            canvas.drawText(line.toString(), margin, yPos, paint)
            line = StringBuilder(word)
            yPos += 20f
        } else {
            line = StringBuilder(testLine)
        }
        
        if (yPos > 800f) break // Page end check
    }
    canvas.drawText(line.toString(), margin, yPos, paint)

    // Footer
    paint.color = android.graphics.Color.GRAY
    paint.textSize = 10f
    canvas.drawText("Documento gerado automaticamente pelo Nexo.", margin, 820f, paint)

    pdfDocument.finishPage(page)

    val fileName = "Nexo_${subjectName.replace(" ", "_")}_${date}.pdf"
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
            putExtra(Intent.EXTRA_SUBJECT, "Anotação: $subjectName")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(shareIntent, "Enviar Anotação..."))

    } catch (e: Exception) {
        Toast.makeText(context, "Erro ao exportar PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        pdfDocument.close()
    }
}
