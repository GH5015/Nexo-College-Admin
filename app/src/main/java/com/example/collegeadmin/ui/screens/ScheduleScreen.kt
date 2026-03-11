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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.collegeadmin.LocalIndicatorClick
import com.example.collegeadmin.model.ClassSession
import com.example.collegeadmin.ui.CollegeViewModel
import com.example.collegeadmin.ui.components.ScreenIndicator
import com.example.collegeadmin.ui.components.EmptyStatePlaceholder
import com.example.collegeadmin.ui.components.HelpPopup
import com.example.collegeadmin.ui.components.HelpItem
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

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
            ClassNotesScreen(session, selectedDate ?: LocalDate.now(), viewModel, onBack = { viewModel.setSelectedSession(null) })
        }
    }
}

@Composable
fun ScheduleList(viewModel: CollegeViewModel, paddingValues: PaddingValues, onSessionClick: (ClassSession, LocalDate) -> Unit) {
    val userInfo by viewModel.userInfo.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val sessions by viewModel.sessions.collectAsState()
    val onMenuClick = LocalIndicatorClick.current

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
                    "Registro de Conteúdo",
                    "Toque em uma aula para escrever o que aprendeu, anexar fotos do quadro ou arquivos em PDF.",
                    Icons.Default.HistoryEdu,
                    Color(0xFF10B981)
                ),
                HelpItem(
                    "Exportar PDF",
                    "Dentro de uma anotação, use o botão de compartilhar para gerar um PDF e salvar no Google Drive ou enviar para colegas.",
                    Icons.Default.PictureAsPdf,
                    Color(0xFFEF4444)
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
                    Icon(Icons.Default.HelpOutline, "Ajuda", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
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

        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
            when(selectedTab) {
                0 -> TodayTab(sessions, onSessionClick)
                1 -> WeeklyTab(sessions, onSessionClick)
                2 -> PeriodTab(sessions, userInfo, onSessionClick)
            }
        }
    }
}

@Composable
fun TodayTab(sessions: List<ClassSession>, onSessionClick: (ClassSession, LocalDate) -> Unit) {
    val today = LocalDate.now()
    val displayList = sessions.filter { it.dayOfWeek == today.dayOfWeek.value }
    if (displayList.isEmpty()) {
        EmptyStatePlaceholder("Nenhuma aula hoje. Bom descanso!")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(displayList) { s -> SessionCardPremium(s, today, onSessionClick) }
        }
    }
}

@Composable
fun WeeklyTab(sessions: List<ClassSession>, onSessionClick: (ClassSession, LocalDate) -> Unit) {
    val days = listOf("Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado")
    val today = LocalDate.now()
    val monday = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
    
    LazyColumn(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        days.forEachIndexed { index, day ->
            val daySessions = sessions.filter { it.dayOfWeek == index + 1 }
            if (daySessions.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            day, 
                            style = MaterialTheme.typography.titleMedium, 
                            color = MaterialTheme.colorScheme.primary, 
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(12.dp))
                        daySessions.forEach { s ->
                            val sessionDate = monday.plusDays(index.toLong())
                            SessionCardPremium(s, sessionDate, onSessionClick)
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PeriodTab(sessions: List<ClassSession>, userInfo: com.example.collegeadmin.model.UserInfo?, onSessionClick: (ClassSession, LocalDate) -> Unit) {
    if (userInfo == null) {
        EmptyStatePlaceholder("Configure seu período letivo no perfil.")
    } else {
        val weeksCount = ChronoUnit.WEEKS.between(userInfo.periodStart, userInfo.periodEnd).toInt()
        LazyColumn(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            (0..weeksCount).forEach { weekIndex ->
                val weekStartDate = userInfo.periodStart.plusWeeks(weekIndex.toLong())
                if (weekStartDate.isBefore(userInfo.periodEnd)) {
                    item {
                        Column {
                            Text(
                                "Semana ${weekIndex + 1}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(Modifier.height(16.dp))
                            sessions.sortedBy { it.dayOfWeek }.forEach { session ->
                                val sessionDate = weekStartDate.plusDays((session.dayOfWeek - 1).toLong())
                                if (!sessionDate.isBefore(userInfo.periodStart) && !sessionDate.isAfter(userInfo.periodEnd)) {
                                    val isPast = sessionDate.isBefore(LocalDate.now())
                                    SessionCardPremium(
                                        session = session,
                                        date = sessionDate,
                                        onClick = onSessionClick,
                                        dateLabel = sessionDate.format(DateTimeFormatter.ofPattern("dd/MM")),
                                        isPast = isPast
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
}

@Composable
fun SessionCardPremium(
    session: ClassSession, 
    date: LocalDate,
    onClick: (ClassSession, LocalDate) -> Unit, 
    dateLabel: String? = null,
    isPast: Boolean = false
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(session, date) },
        shape = RoundedCornerShape(24.dp),
        color = if (isPast) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface,
        tonalElevation = if (isPast) 0.dp else 2.dp
    ) {
        Row(
            modifier = Modifier.padding(20.dp), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp)) {
                if(dateLabel != null) {
                    Text(dateLabel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Text(session.startTime.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(session.endTime.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            
            Spacer(Modifier.width(20.dp))
            
            Box(modifier = Modifier.width(1.dp).height(40.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)))
            
            Spacer(Modifier.width(20.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    session.subjectName, 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold,
                    color = if (isPast) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MeetingRoom, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.width(4.dp))
                    Text(session.room, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }
            
            if (isPast) {
                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
            } else {
                Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassNotesScreen(session: ClassSession, date: LocalDate, viewModel: CollegeViewModel, onBack: () -> Unit) {
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
                        IconButton(onClick = { exportAndShareNotePdf(context, note.title, note.content) }) {
                            Icon(Icons.Default.Share, null, tint = MaterialTheme.colorScheme.primary)
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
                            Text("Arquivos Anexados", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            note.attachments.forEach { fileUri ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { 
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                data = Uri.parse(fileUri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) { }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.AutoMirrored.Filled.InsertDriveFile, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            Uri.parse(fileUri).lastPathSegment ?: "Arquivo", 
                                            style = MaterialTheme.typography.labelLarge,
                                            maxLines = 1
                                        )
                                    }
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

private fun exportAndShareNotePdf(context: Context, title: String, content: String) {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() 
    val page = pdfDocument.startPage(pageInfo)
    val canvas: Canvas = page.canvas
    val paint = Paint()

    paint.textSize = 20f
    paint.isFakeBoldText = true
    canvas.drawText(title, 50f, 50f, paint)

    paint.textSize = 12f
    paint.isFakeBoldText = false
    paint.color = android.graphics.Color.GRAY
    canvas.drawText("Gerado em: ${LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}", 50f, 80f, paint)

    paint.color = android.graphics.Color.BLACK
    paint.textSize = 14f
    val lines = content.split("\n")
    var yPos = 120f
    for (line in lines) {
        if (yPos > 800f) break 
        canvas.drawText(line, 50f, yPos, paint)
        yPos += 20f
    }

    pdfDocument.finishPage(page)

    val fileName = "Anotacao_${title.replace(" ", "_")}.pdf"
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
            putExtra(Intent.EXTRA_SUBJECT, "Anotação Acadêmica: $title")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(shareIntent, "Compartilhar PDF via..."))

    } catch (e: Exception) {
        Toast.makeText(context, "Erro ao gerar PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        pdfDocument.close()
    }
}
