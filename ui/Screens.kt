package com.example.collegeadmin.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.collegeadmin.AppDestinations
import com.example.collegeadmin.model.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

// --- DASHBOARD ---
@Composable
fun DashboardScreen(
    viewModel: CollegeViewModel, 
    paddingValues: PaddingValues, 
    onNavigate: (AppDestinations) -> Unit
) {
    val subjects by viewModel.subjects.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val events by viewModel.events.collectAsState()
    val userInfo by viewModel.userInfo.collectAsState()
    
    val upcomingTasks = events.filter { it.date.isAfter(LocalDate.now().minusDays(1)) }.take(3)
    
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = "Olá, ${userInfo?.name ?: "Estudante"} 👋", 
                    style = MaterialTheme.typography.headlineMedium, 
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))),
                    style = MaterialTheme.typography.bodyMedium, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // --- PRÓXIMAS TAREFAS ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Task, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PRÓXIMAS TAREFAS", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (upcomingTasks.isEmpty()) {
                        Text("Nenhum compromisso pendente.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        upcomingTasks.forEach { event ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clickable { onNavigate(AppDestinations.ROUTINE) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if(event.type == EventType.EXAM) Icons.Default.PriorityHigh else Icons.AutoMirrored.Filled.Assignment, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(16.dp),
                                    tint = if(event.type == EventType.EXAM) Color.Red else MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(event.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                    Text(event.date.format(DateTimeFormatter.ofPattern("dd 'de' MMM")), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                item { SummaryCard("Disciplinas", subjects.size.toString(), Icons.AutoMirrored.Filled.MenuBook, MaterialTheme.colorScheme.secondary) }
                item { SummaryCard("Trabalhos", events.count { it.type == EventType.ASSIGNMENT }.toString(), Icons.AutoMirrored.Filled.List, Color(0xFF6750A4)) }
                item { SummaryCard("Faltas", subjects.sumOf { it.absences }.toString(), Icons.Default.Warning, MaterialTheme.colorScheme.error) }
            }
        }

        item { Text("Minhas Aulas de Hoje", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        
        val today = LocalDate.now().dayOfWeek.value
        val todaySessions = sessions.filter { it.dayOfWeek == today }
        
        if (todaySessions.isEmpty()) {
            item { Text("Não há aulas programadas para hoje.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline) }
        } else {
            items(todaySessions) { session ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNavigate(AppDestinations.SCHEDULE) },
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.School, null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(session.subjectName, fontWeight = FontWeight.Bold)
                            Text("${session.startTime} - ${session.endTime}", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
        
        item { Spacer(Modifier.height(16.dp)) }
    }
}

// --- ONBOARDING ---
@Composable
fun OnboardingScreen(onComplete: (String, String, String, LocalDate, LocalDate) -> Unit) {
    var name by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("") }
    var shift by remember { mutableStateOf("") }
    var startStr by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)) }
    var endStr by remember { mutableStateOf(LocalDate.now().plusMonths(5).format(DateTimeFormatter.ISO_LOCAL_DATE)) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.School, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))
        Text("Bem-vindo ao College Admin", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text("Configure seu perfil para começar", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        
        Spacer(Modifier.height(32.dp))
        TextField(value = name, onValueChange = { name = it }, label = { Text("Nome Completo") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        TextField(value = course, onValueChange = { course = it }, label = { Text("Curso") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        TextField(value = shift, onValueChange = { shift = it }, label = { Text("Turno (Ex: Manhã, Tarde, Noite)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        TextField(value = startStr, onValueChange = { startStr = it }, label = { Text("Data Início Período (AAAA-MM-DD)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        TextField(value = endStr, onValueChange = { endStr = it }, label = { Text("Data Fim Período (AAAA-MM-DD)") }, modifier = Modifier.fillMaxWidth())
        
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                if (name.isNotBlank() && course.isNotBlank()) {
                    try {
                        onComplete(name, course, shift, LocalDate.parse(startStr), LocalDate.parse(endStr))
                    } catch(e: Exception) {}
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Concluir Cadastro")
        }
    }
}

@Composable
fun SummaryCard(title: String, value: String, icon: ImageVector, color: Color) {
    Surface(modifier = Modifier.width(130.dp).height(100.dp), shape = RoundedCornerShape(20.dp), color = color.copy(alpha = 0.1f) ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Column {
                Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = color)
                Text(title, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f))
            }
        }
    }
}

// --- DISCIPLINAS ---
@Composable
fun SubjectsScreen(viewModel: CollegeViewModel, paddingValues: PaddingValues) {
    val subjects by viewModel.subjects.collectAsState()
    var selectedSubject by remember { mutableStateOf<Subject?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var subjectToEdit by remember { mutableStateOf<Subject?>(null) }

    Crossfade(targetState = selectedSubject, label = "SubjectNav") { subject ->
        if (subject == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item { Text("Minhas Disciplinas", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 16.dp)) }
                    items(subjects) { sub ->
                        SubjectCardModern(
                            sub, 
                            onEdit = { subjectToEdit = sub },
                            onDelete = { viewModel.deleteSubject(sub.id) }
                        ) { selectedSubject = sub }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
                FloatingActionButton(onClick = { showAddDialog = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
                    Icon(Icons.Default.Add, null)
                }
            }
        } else {
            val currentSub = subjects.find { it.id == subject.id }
            if (currentSub != null) SubjectDetailScreen(currentSub, viewModel, onBack = { selectedSubject = null })
            else selectedSubject = null
        }
    }

    if (showAddDialog) {
        SubjectDialog(onDismiss = { showAddDialog = false }) { name, prof, room, day, start, end ->
            viewModel.addSubject(name, prof, room, 40, day, start, end)
        }
    }

    if (subjectToEdit != null) {
        SubjectDialog(
            subject = subjectToEdit,
            onDismiss = { subjectToEdit = null }
        ) { name, prof, room, _, _, _ ->
            viewModel.editSubject(subjectToEdit!!.id, name, prof, room, 40)
        }
    }
}

@Composable
fun SubjectDialog(
    subject: Subject? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Int, LocalTime, LocalTime) -> Unit
) {
    var name by remember { mutableStateOf(subject?.name ?: "") }
    var prof by remember { mutableStateOf(subject?.professor ?: "") }
    var room by remember { mutableStateOf(subject?.room ?: "") }
    var selectedDay by remember { mutableIntStateOf(1) }
    var startHour by remember { mutableStateOf("08:00") }
    var endHour by remember { mutableStateOf("10:00") }
    
    val days = listOf("Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if(subject == null) "Nova Disciplina" else "Editar Disciplina") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { TextField(value = name, onValueChange = { name = it }, label = { Text("Nome da Disciplina") }, modifier = Modifier.fillMaxWidth()) }
                item { TextField(value = prof, onValueChange = { prof = it }, label = { Text("Professor") }, modifier = Modifier.fillMaxWidth()) }
                item { TextField(value = room, onValueChange = { room = it }, label = { Text("Sala") }, modifier = Modifier.fillMaxWidth()) }
                
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
                            TextField(value = startHour, onValueChange = { startHour = it }, label = { Text("Início") }, modifier = Modifier.weight(1f))
                            TextField(value = endHour, onValueChange = { endHour = it }, label = { Text("Fim") }, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        },
        confirmButton = { 
            TextButton(onClick = { 
                if(name.isNotBlank()) {
                    try {
                        val start = LocalTime.parse(startHour)
                        val end = LocalTime.parse(endHour)
                        onConfirm(name, prof, room, selectedDay, start, end)
                        onDismiss()
                    } catch (e: Exception) { }
                }
            }) { Text(if(subject == null) "Adicionar" else "Salvar") } 
        }
    )
}

@Composable
fun SubjectCardModern(subject: Subject, onEdit: () -> Unit, onDelete: () -> Unit, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(subject.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary) }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                }
            }
            Text(subject.professor, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(12.dp))
            val presence = (1.0f - (subject.absences.toFloat() / subject.totalClasses.toFloat())).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { presence }, 
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = if (subject.isFailedByAbsence) MaterialTheme.colorScheme.error 
                        else if (subject.isAtRiskOfAbsence) Color(0xFFF1C40F) 
                        else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Presença: ${(presence * 100).toInt()}% (Mín: 75%)", style = MaterialTheme.typography.labelSmall, 
                    color = if(subject.isFailedByAbsence) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                Text("Média: ${String.format(Locale.getDefault(), "%.1f", subject.averageGrade)}", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(subject: Subject, viewModel: CollegeViewModel, onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Notas", "Faltas", "Agenda", "Geral")
    
    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text(subject.name, fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } })
                TabRow(selectedTabIndex = tab) {
                    tabs.forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) }) }
                }
            }
        }
    ) { p ->
        Column(modifier = Modifier.padding(p).padding(16.dp)) {
            when(tab) {
                0 -> SubjectGradesTab(subject, viewModel)
                1 -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Faltas Totais", style = MaterialTheme.typography.labelLarge)
                    Text("${subject.absences}", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black, 
                        color = if(subject.isFailedByAbsence) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                    Text("Limite permitido: ${(subject.totalClasses * 0.25).toInt()} (Presença Mín: 75%)", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.addAbsence(subject.id) }) { Text("+ Falta") }
                        if (subject.absences > 0) OutlinedButton(onClick = { viewModel.removeAbsence(subject.id) }) { Text("Remover") }
                    }
                }
                2 -> SubjectEventsTab(subject, viewModel)
                3 -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    InfoDetailRow(Icons.Default.Person, "Professor", subject.professor)
                    InfoDetailRow(Icons.Default.MeetingRoom, "Sala", subject.room)
                    InfoDetailRow(Icons.Default.Timeline, "Probabilidade de Aprovação", "${String.format(Locale.getDefault(), "%.0f", subject.passingProbability)}%")
                    if (subject.isFailedByAbsence) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Text("Reprovado por Falta: Você ultrapassou o limite de 25% de ausências.", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    } else if (subject.needsPF) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Text("Atenção: Você está de Prova Final (Média atual abaixo de 7.0)", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubjectGradesTab(subject: Subject, viewModel: CollegeViewModel) {
    var p1 by remember { mutableStateOf(subject.p1Grade?.toString() ?: "") }
    var p1Date by remember { mutableStateOf(subject.p1Date?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "") }
    var p2 by remember { mutableStateOf(subject.p2Grade?.toString() ?: "") }
    var p2Date by remember { mutableStateOf(subject.p2Date?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "") }
    var pf by remember { mutableStateOf(subject.pfGrade?.toString() ?: "") }
    var pfDate by remember { mutableStateOf(subject.pfDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "") }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text("Notas e Datas das Avaliações", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        
        GradeInputRow("P1", p1, p1Date, { p1 = it }, { p1Date = it })
        GradeInputRow("P2", p2, p2Date, { p2 = it }, { p2Date = it })
        
        if (subject.needsPF || subject.pfGrade != null) {
            GradeInputRow("PF", pf, pfDate, { pf = it }, { pfDate = it })
        }

        Button(onClick = { 
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            viewModel.updateGrades(
                subject.id, 
                p1.toDoubleOrNull(), try { LocalDate.parse(p1Date, formatter) } catch(e:Exception) { null },
                p2.toDoubleOrNull(), try { LocalDate.parse(p2Date, formatter) } catch(e:Exception) { null },
                pf.toDoubleOrNull(), try { LocalDate.parse(pfDate, formatter) } catch(e:Exception) { null }
            )
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Salvar Notas")
        }

        HorizontalDivider()
        Text("Média Calculada: ${String.format(Locale.getDefault(), "%.1f", subject.averageGrade)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
            color = if (subject.isPassed) Color(0xFF2ECC71) else MaterialTheme.colorScheme.error)
    }
}

@Composable
fun GradeInputRow(label: String, grade: String, date: String, onGradeChange: (String) -> Unit, onDateChange: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        TextField(value = grade, onValueChange = onGradeChange, label = { Text("$label (Nota)") }, modifier = Modifier.weight(1f))
        TextField(value = date, onValueChange = onDateChange, label = { Text("Data (DD/MM/AAAA)") }, modifier = Modifier.weight(1.5f))
    }
}

@Composable
fun SubjectEventsTab(subject: Subject, viewModel: CollegeViewModel) {
    var showAddEvent by remember { mutableStateOf(false) }
    // Apenas trabalhos extras, provas extras agora são inseridas em "Notas"
    val assignments = subject.assignments.filter { it.type == EventType.ASSIGNMENT }
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Trabalhos Extra", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = { showAddEvent = true }) { Icon(Icons.Default.AddCircle, null, tint = MaterialTheme.colorScheme.primary) }
        }
        
        if (assignments.isNotEmpty()) {
            assignments.forEach { ass -> EventItem(ass, onDelete = { viewModel.deleteAssignment(ass.id) }) }
        }
        
        if (assignments.isEmpty()) {
            Text("Nenhum trabalho extra cadastrado.", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.outline)
        }
    }

    if (showAddEvent) {
        var title by remember { mutableStateOf("") }
        var dateStr by remember { mutableStateOf(LocalDate.now().plusDays(7).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))) }
        
        AlertDialog(
            onDismissRequest = { showAddEvent = false },
            title = { Text("Novo Trabalho") },
            text = { 
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(value = title, onValueChange = { title = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
                    TextField(value = dateStr, onValueChange = { dateStr = it }, label = { Text("Data (DD/MM/AAAA)") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = { TextButton(onClick = { 
                try {
                    val date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    viewModel.addAssignment(subject.id, title, date, EventType.ASSIGNMENT)
                    showAddEvent = false
                } catch(e: Exception) { }
            }) { Text("Adicionar") } }
        )
    }
}

@Composable
fun EventItem(ass: Assignment, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(ass.title, fontWeight = FontWeight.Bold) }, 
            trailingContent = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(ass.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) }
                }
            },
            leadingContent = { Icon(Icons.AutoMirrored.Filled.Assignment, null) }
        )
    }
}

@Composable
fun InfoDetailRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        }
    }
}

// --- AULAS ---
@Composable
fun ScheduleScreen(viewModel: CollegeViewModel, paddingValues: PaddingValues) {
    val selectedSession by viewModel.selectedSession.collectAsState()
    Crossfade(targetState = selectedSession, label = "ScheduleNav") { session ->
        if (session == null) ScheduleList(viewModel, paddingValues) { viewModel.setSelectedSession(it) }
        else ClassNotesScreen(session, viewModel, onBack = { viewModel.setSelectedSession(null) })
    }
}

@Composable
fun ScheduleList(viewModel: CollegeViewModel, paddingValues: PaddingValues, onSessionClick: (ClassSession) -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    val sessions by viewModel.sessions.collectAsState()
    val userInfo by viewModel.userInfo.collectAsState()

    Column(modifier = Modifier.padding(paddingValues)) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Hoje") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Semana") })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Período") })
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            when(tab) {
                0 -> {
                    val displayList = sessions.filter { it.dayOfWeek == LocalDate.now().dayOfWeek.value }
                    if (displayList.isEmpty()) {
                        item { Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) { Text("Sem aulas programadas.") } }
                    } else {
                        items(displayList) { s -> ClassCardProfessional(s, onSessionClick) }
                    }
                }
                1 -> {
                    val days = listOf("Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado")
                    days.forEachIndexed { index, day ->
                        val daySessions = sessions.filter { it.dayOfWeek == index + 1 }
                        if (daySessions.isNotEmpty()) {
                            item { Text(day, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
                            items(daySessions) { s -> ClassCardProfessional(s, onSessionClick) }
                        }
                    }
                }
                2 -> {
                    if (userInfo == null) {
                        item { Text("Configure seu período no perfil.") }
                    } else {
                        val weeksCount = ChronoUnit.WEEKS.between(userInfo!!.periodStart, userInfo!!.periodEnd).toInt()
                        (0..weeksCount).forEach { weekIndex ->
                            val weekStartDate = userInfo!!.periodStart.plusWeeks(weekIndex.toLong())
                            if (weekStartDate.isBefore(userInfo!!.periodEnd)) {
                                item { 
                                    Text(
                                        text = "Semana ${weekIndex + 1}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(top = 16.dp)
                                    )
                                }
                                
                                sessions.sortedBy { it.dayOfWeek }.forEach { session ->
                                    // Calculate the specific date for this session in this week
                                    // Correct day adjustment: find the date of the 'dayOfWeek' within that week
                                    // weekStartDate is always a Monday? Not necessarily. Let's adjust based on dayOfWeek.
                                    val sessionDate = weekStartDate.plusDays((session.dayOfWeek - 1).toLong())
                                    
                                    if (!sessionDate.isBefore(userInfo!!.periodStart) && !sessionDate.isAfter(userInfo!!.periodEnd)) {
                                        item {
                                            val isPast = sessionDate.isBefore(LocalDate.now())
                                            ClassCardProfessional(
                                                session = session,
                                                onClick = onSessionClick,
                                                showDay = true,
                                                dateLabel = sessionDate.format(DateTimeFormatter.ofPattern("dd/MM")),
                                                isPast = isPast
                                            )
                                        }
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
}

@Composable
fun ClassCardProfessional(
    session: ClassSession, 
    onClick: (ClassSession) -> Unit, 
    showDay: Boolean = false,
    dateLabel: String? = null,
    isPast: Boolean = false
) {
    val days = listOf("", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick(session) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPast) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) 
                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(65.dp)) {
                if(showDay) Text(dateLabel ?: days[session.dayOfWeek], style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text(session.startTime.toString(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(session.endTime.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            VerticalDivider(modifier = Modifier.height(40.dp).padding(horizontal = 16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(session.subjectName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Text("Sala Central", style = MaterialTheme.typography.bodySmall)
            }
            if (isPast) Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
            else Icon(Icons.AutoMirrored.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassNotesScreen(session: ClassSession, viewModel: CollegeViewModel, onBack: () -> Unit) {
    val allNotes by viewModel.notes.collectAsState()
    val note = allNotes[session.subjectId]
    var showEditNote by remember { mutableStateOf(false) }
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val newAttachments = uris.map { it.toString() }
            viewModel.saveNote(
                session.subjectId, 
                note?.title ?: "Anotação de ${session.subjectName}", 
                note?.content ?: "", 
                (note?.attachments ?: emptyList()) + newAttachments
            )
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(session.subjectName) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }) },
        floatingActionButton = { 
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallFloatingActionButton(onClick = { filePickerLauncher.launch("*/*") }, containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    Icon(Icons.Default.AttachFile, null)
                }
                ExtendedFloatingActionButton(
                    onClick = { showEditNote = true },
                    icon = { Icon(if (note == null) Icons.Default.NoteAdd else Icons.Default.EditNote, null) },
                    text = { Text(if (note == null) "Criar Anotação" else "Editar Texto") }
                )
            }
        }
    ) { p ->
        Column(modifier = Modifier.padding(p).padding(16.dp).fillMaxSize()) {
            if (note == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhuma anotação central para esta disciplina.", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                        Text(note.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Última atualização: ${note.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(16.dp))
                        Text(note.content, style = MaterialTheme.typography.bodyLarge)
                        
                        if (note.attachments.isNotEmpty()) {
                            Spacer(Modifier.height(24.dp))
                            Text("Arquivos Anexados", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            note.attachments.forEach { fileUri ->
                                ListItem(
                                    headlineContent = { Text(fileUri.substringAfterLast("/")) },
                                    supportingContent = { Text(fileUri, maxLines = 1) },
                                    leadingContent = { Icon(Icons.AutoMirrored.Filled.InsertDriveFile, null) },
                                    modifier = Modifier.clickable { /* Intent para abrir URI */ }
                                )
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(value = title, onValueChange = { title = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
                    TextField(value = content, onValueChange = { content = it }, label = { Text("Conteúdo") }, modifier = Modifier.fillMaxWidth(), minLines = 5)
                }
            },
            confirmButton = { 
                TextButton(onClick = { 
                    viewModel.saveNote(session.subjectId, title, content, note?.attachments ?: emptyList())
                    showEditNote = false 
                }) { Text("Salvar Texto") } 
            }
        )
    }
}

// --- ROTINA ---
@Composable
fun RoutineScreen(viewModel: CollegeViewModel, paddingValues: PaddingValues) {
    val events by viewModel.events.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text("Minha Rotina", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 16.dp)) }
            
            items(events) { e ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if(e.type == EventType.EXAM) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant)) {
                    ListItem(
                        headlineContent = { Text(e.title, fontWeight = FontWeight.Bold) }, 
                        supportingContent = { Text(e.description) },
                        overlineContent = { Text(e.type.name) }, 
                        trailingContent = { Text(e.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
            
            item { Text("Tarefas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            items(tasks) { t ->
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { viewModel.toggleTask(t.id) }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = t.isCompleted, onCheckedChange = { viewModel.toggleTask(t.id) })
                    Text(t.title, modifier = Modifier.weight(1f), textDecoration = if (t.isCompleted) TextDecoration.LineThrough else null)
                    IconButton(onClick = { viewModel.deleteTask(t.id) }) { Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp)) }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
        FloatingActionButton(onClick = { showAdd = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) { Icon(Icons.Default.AddTask, null) }
    }
    if (showAdd) {
        var title by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { showAdd = false }, title = { Text("Nova Tarefa") },
            text = { TextField(value = title, onValueChange = { title = it }, label = { Text("O que precisa fazer?") }) },
            confirmButton = { TextButton(onClick = { if(title.isNotBlank()) viewModel.addTask(title, ""); showAdd = false }) { Text("Salvar") } })
    }
}

// --- PERFIL ---
@Composable
fun ProfileScreen(viewModel: CollegeViewModel, paddingValues: PaddingValues) {
    val userInfo by viewModel.userInfo.collectAsState()
    
    LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item {
            Surface(modifier = Modifier.size(100.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary) }
            }
            Text(userInfo?.name ?: "Usuário", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("${userInfo?.course ?: "Curso não definido"} • ${userInfo?.shift ?: "Turno"}", style = MaterialTheme.typography.bodyMedium)
            Text("Início: ${userInfo?.periodStart?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "-"} • Fim: ${userInfo?.periodEnd?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "-"}", 
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    ListItem(headlineContent = { Text("Notificações") }, trailingContent = { Switch(checked = true, onCheckedChange = {}) })
                    ListItem(headlineContent = { Text("Tema Escuro") }, leadingContent = { Icon(Icons.Default.DarkMode, null) }, trailingContent = { Switch(checked = false, onCheckedChange = {}) })
                }
            }
        }
    }
}
