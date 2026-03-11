package com.example.collegeadmin.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.collegeadmin.LocalIndicatorClick
import com.example.collegeadmin.ai.AiAssistant
import com.example.collegeadmin.model.*
import com.example.collegeadmin.ui.CollegeViewModel
import com.example.collegeadmin.data.local.GeneratedReviewEntity
import com.example.collegeadmin.ui.components.ScreenIndicator
import com.example.collegeadmin.ui.components.EmptyStatePlaceholder
import com.example.collegeadmin.ui.components.HelpPopup
import com.example.collegeadmin.ui.components.HelpItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineScreen(viewModel: CollegeViewModel, paddingValues: PaddingValues) {
    val userInfo by viewModel.userInfo.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showStudyTab by remember { mutableStateOf(false) }
    val tabs = listOf("Afazeres", "Plano de Estudos", "Foco em Provas")
    val onMenuClick = LocalIndicatorClick.current

    var showHelp by remember { mutableStateOf(false) }

    LaunchedEffect(userInfo?.showHelp) {
        if (userInfo?.showHelp == true) {
            showHelp = true
        }
    }

    if (showHelp) {
        HelpPopup(
            title = "Gestão de Estudos",
            helpItems = listOf(
                HelpItem(
                    "Lista de Afazeres",
                    "Organize suas tarefas diárias. Marque o que já foi feito para manter o foco.",
                    Icons.Default.Checklist,
                    MaterialTheme.colorScheme.primary
                ),
                HelpItem(
                    "Plano de Estudos IA",
                    "A IA cria um cronograma personalizado baseado nas suas provas e conteúdos. Basta um toque!",
                    Icons.Default.AutoAwesome,
                    Color(0xFF8B5CF6)
                ),
                HelpItem(
                    "Repetição Espaçada",
                    "No botão inferior esquerdo, você acessa seu cronograma de revisões inteligentes para nunca esquecer o que aprendeu.",
                    Icons.Default.History,
                    Color(0xFF10B981)
                )
            ),
            onDismiss = { showHelp = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(paddingValues)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ScreenIndicator(
                        label = "Estudos", 
                        color = MaterialTheme.colorScheme.primary,
                        onClick = onMenuClick
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { showHelp = true }) {
                        Icon(Icons.Default.HelpOutline, "Ajuda", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
                    }
                }
            }
            
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                containerColor = Color.Transparent,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]).padding(horizontal = 8.dp).clip(CircleShape),
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

            Box(modifier = Modifier.fillMaxSize()) {
                when(selectedTab) {
                    0 -> GeneralRoutineTab(viewModel)
                    1 -> StudyPlanTab(viewModel)
                    2 -> ExamsStudyTab(viewModel)
                }
            }
        }

        SmallFloatingActionButton(
            onClick = { showStudyTab = true },
            modifier = Modifier.align(Alignment.BottomStart).padding(24.dp),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Insights, "Estudos Diários")
        }
    }

    if (showStudyTab) {
        ModalBottomSheet(
            onDismissRequest = { showStudyTab = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            StudyTab(viewModel, onDismiss = { showStudyTab = false })
        }
    }
}

@Composable
fun GeneralRoutineTab(viewModel: CollegeViewModel) {
    val events by viewModel.events.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    var showAddTaskDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), 
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { 
                Text(
                    text = "Minha Rotina", 
                    style = MaterialTheme.typography.headlineLarge, 
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                    fontWeight = FontWeight.ExtraBold
                ) 
            }
            
            if (events.isNotEmpty()) {
                item {
                    Text(
                        "Próximos Compromissos", 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(events) { event ->
                    AcademicEventCardPremium(event)
                }
            }
            
            item { 
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Lista de Afazeres", 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${tasks.count { it.isCompleted }}/${tasks.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            if (tasks.isEmpty()) {
                item {
                    EmptyStatePlaceholder("Nenhuma tarefa pendente.")
                }
            } else {
                items(tasks) { task ->
                    RoutineTaskItemPremium(
                        task = task,
                        onToggle = { viewModel.toggleTask(task.id) },
                        onDelete = { viewModel.deleteTask(task.id) }
                    )
                }
            }
            
            item { Spacer(Modifier.height(100.dp)) }
        }
        
        FloatingActionButton(
            onClick = { showAddTaskDialog = true }, 
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) { 
            Icon(Icons.Default.Add, contentDescription = "Nova Tarefa") 
        }
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title ->
                viewModel.addTask(title, "")
                showAddTaskDialog = false
            }
        )
    }
}

@Composable
fun ExamsStudyTab(viewModel: CollegeViewModel) {
    val events by viewModel.events.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val futureExams = events.filter { it.type == EventType.EXAM && it.date.isAfter(LocalDate.now().minusDays(1)) }
    
    val scope = rememberCoroutineScope()
    val apiKey = "AIzaSyAQMyzTBaQ8zBy7J_-sRF84zpbuPhZHyNU"
    val assistant = remember { AiAssistant(apiKey) }
    
    var selectedExam by remember { mutableStateOf<AcademicEvent?>(null) }
    var examSummary by remember { mutableStateOf("") }
    val explanations = remember { mutableStateListOf<Pair<String, String>>() }
    var userAnotations by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isTyping by remember { mutableStateOf(false) }

    if (selectedExam == null) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Text("Provas Futuras", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            if (futureExams.isEmpty()) {
                EmptyStatePlaceholder("Nenhuma prova agendada para os próximos dias.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(futureExams) { exam ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { 
                                selectedExam = exam
                                examSummary = ""
                                explanations.clear()
                                userAnotations = ""
                                
                                viewModel.getStudySummary(exam.id) { saved ->
                                    if (saved == null) {
                                        isLoading = true
                                        scope.launch {
                                            val subjectNotes = notes.filter { it.subjectId == exam.subjectId }
                                            val content = if (subjectNotes.isEmpty()) "Conteúdo geral de ${exam.title}"
                                                         else subjectNotes.joinToString("\n") { "${it.title}: ${it.content}" }
                                            
                                            isLoading = false
                                            assistant.askTutorStream("Crie um resumo de estudo estruturado para a prova: ${exam.title}. Conteúdo: $content")
                                                .onCompletion {
                                                    viewModel.saveStudySummary(exam.id, examSummary, emptyList(), "")
                                                }
                                                .collect { chunk ->
                                                    examSummary += chunk
                                                }
                                        }
                                    } else {
                                        examSummary = saved.baseSummary
                                        explanations.addAll(saved.additionalExplanations)
                                        userAnotations = saved.userNotes
                                    }
                                }
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp
                        ) {
                            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Event, null, tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(exam.title, fontWeight = FontWeight.Bold)
                                    Text(exam.date.format(DateTimeFormatter.ofPattern("dd 'de' MMMM")), style = MaterialTheme.typography.labelSmall)
                                }
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        }
    } else {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectedExam = null; examSummary = ""; explanations.clear() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                }
                Text(selectedExam!!.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            
            Spacer(Modifier.height(16.dp))
            
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    Text("Resumo Base", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    
                    SelectionContainer {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(examSummary, style = MaterialTheme.typography.bodyMedium)
                                
                                Spacer(Modifier.height(12.dp))
                                
                                Text(
                                    "Selecione um trecho e use o botão abaixo para explicar melhor.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    if (explanations.isNotEmpty()) {
                        Spacer(Modifier.height(24.dp))
                        Text("Aprofundamentos", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                        explanations.forEach { (point, explanation) ->
                            Spacer(Modifier.height(12.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("Sobre: \"$point\"", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(8.dp))
                                    Text(explanation, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    
                    if (isTyping) {
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }

                    Spacer(Modifier.height(24.dp))
                    Text("Minhas Anotações", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = userAnotations,
                        onValueChange = { 
                            userAnotations = it 
                            viewModel.saveStudySummary(selectedExam!!.id, examSummary, explanations.toList(), it)
                        },
                        placeholder = { Text("Escreva suas notas de estudo aqui...") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    
                    Spacer(Modifier.height(80.dp))
                }
                
                var showExplainDialog by remember { mutableStateOf(false) }
                var textToExplain by remember { mutableStateOf("") }

                Button(
                    onClick = { showExplainDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isTyping
                ) {
                    Icon(Icons.Default.Psychology, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Explique um ponto específico")
                }

                if (showExplainDialog) {
                    AlertDialog(
                        onDismissRequest = { showExplainDialog = false },
                        title = { Text("O que deseja entender melhor?") },
                        text = {
                            OutlinedTextField(
                                value = textToExplain,
                                onValueChange = { textToExplain = it },
                                placeholder = { Text("Cole ou digite o termo aqui...") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        },
                        confirmButton = {
                            Button(onClick = {
                                if (textToExplain.isNotBlank()) {
                                    isTyping = true
                                    val point = textToExplain
                                    var currentExplanation = ""
                                    explanations.add(point to "")
                                    val index = explanations.size - 1
                                    
                                    scope.launch {
                                        assistant.explainSpecificPointStream(examSummary, point)
                                            .onCompletion {
                                                viewModel.saveStudySummary(selectedExam!!.id, examSummary, explanations.toList(), userAnotations)
                                                isTyping = false
                                            }
                                            .collect { chunk ->
                                                currentExplanation += chunk
                                                explanations[index] = point to currentExplanation
                                            }
                                    }
                                }
                                showExplainDialog = false
                                textToExplain = ""
                            }) { Text("Explique") }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StudyPlanTab(viewModel: CollegeViewModel) {
    val events by viewModel.events.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    
    val scope = rememberCoroutineScope()
    val apiKey = "AIzaSyAQMyzTBaQ8zBy7J_-sRF84zpbuPhZHyNU"
    val assistant = remember { AiAssistant(apiKey) }
    
    var studyPlan by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.getStudyPlan { saved ->
            if (saved != null) {
                studyPlan = saved.content
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text("Mentor de Estudos IA", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "A IA analisará suas próximas provas e o conteúdo das aulas para sugerir um cronograma otimizado.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        isLoading = true
                        scope.launch {
                            val exams = events.filter { it.type == EventType.EXAM }
                                .joinToString("\n") { "${it.title} em ${it.date}" }
                            
                            val content = subjects.joinToString("\n\n") { subject ->
                                val subjectNotes = notes.filter { it.subjectId == subject.id }
                                "MATÉRIA: ${subject.name}\nCONTEÚDO:\n" + 
                                (if (subjectNotes.isEmpty()) "Sem notas registradas." 
                                 else subjectNotes.joinToString("; ") { it.title + ": " + it.content })
                            }
                            
                            val generatedPlan = assistant.suggestStudyPlan(exams, content)
                            studyPlan = generatedPlan
                            viewModel.saveStudyPlan(generatedPlan)
                            isLoading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("Gerar Novo Plano Personalizado")
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        if (studyPlan != null) {
            Surface(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                    Text(
                        text = studyPlan!!,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )
                }
            }
        } else if (!isLoading) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.AutoStories, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(Modifier.height(16.dp))
                Text("Toque no botão acima para organizar sua semana.", color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyTab(viewModel: CollegeViewModel, onDismiss: () -> Unit) {
    val studyList by viewModel.studyList.collectAsState()
    val overdue by viewModel.overdueStudySessions.collectAsState()
    val forToday by viewModel.todayStudySessions.collectAsState()
    val upcoming by viewModel.upcomingStudySessions.collectAsState()
    
    // Simulação de carregamento (Skeleton)
    var isProcessing by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(800) // Pequeno delay para o skeleton ser visível
        isProcessing = false
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(16.dp))
        Text(
            "Minha Jornada de Estudo",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Análise de retenção e repetição espaçada.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        if (isProcessing) {
            StudySkeletonLoading()
        } else if (studyList.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.HistoryEdu, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(Modifier.height(16.dp))
                    Text("Comece a anotar suas aulas para ver seu cronograma.", color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Criar minha primeira nota")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                if (overdue.isNotEmpty()) {
                    item { StudySectionHeader("Atrasadas", Color(0xFFEF4444)) }
                    items(overdue) { session -> StudyReviewCard(session, viewModel) }
                }

                if (forToday.isNotEmpty()) {
                    item { StudySectionHeader("Para Hoje", Color(0xFFF59E0B)) }
                    items(forToday) { session -> StudyReviewCard(session, viewModel) }
                }

                if (upcoming.isNotEmpty()) {
                    item { StudySectionHeader("Próximas Revisões", MaterialTheme.colorScheme.primary) }
                    items(upcoming) { session -> StudyReviewCard(session, viewModel) }
                }
            }
        }
    }
}

@Composable
fun StudySkeletonLoading() {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
            )
        }
    }
}

@Composable
fun StudySectionHeader(title: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
        Box(Modifier.size(4.dp, 16.dp).background(color, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = color)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyReviewCard(session: StudySession, viewModel: CollegeViewModel) {
    var isReviewing by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val daysUntil = ChronoUnit.DAYS.between(today, session.reviewDate)
    
    val statusColor = when {
        daysUntil < 0 -> Color(0xFFEF4444)
        daysUntil == 0L -> Color(0xFFF59E0B)
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = Modifier.fillMaxWidth().clickable { isReviewing = true },
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (daysUntil < 0) Icons.Default.EventBusy else if (daysUntil == 0L) Icons.Default.School else Icons.Default.Update,
                        null,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column(Modifier.weight(1f)) {
                    Text(session.note.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text(session.subject?.name ?: "Matéria", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = when {
                            daysUntil < 0 -> "ATRASADA"
                            daysUntil == 0L -> "HOJE"
                            else -> "EM $daysUntil DIAS"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Retention Index Visual
            Row(verticalAlignment = Alignment.CenterVertically) {
                val retention = session.note.retentionIndex
                val retentionColor = when {
                    retention >= 80 -> Color(0xFF10B981)
                    retention >= 50 -> Color(0xFFF59E0B)
                    else -> Color(0xFFEF4444)
                }

                Column(Modifier.weight(1f)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Índice de Retenção", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text("${retention.toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = retentionColor)
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (retention / 100.0).toFloat() },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = retentionColor,
                        trackColor = retentionColor.copy(alpha = 0.1f)
                    )
                }
                
                Spacer(Modifier.width(16.dp))
                
                // Difficulty Badge
                Surface(
                    color = when(session.note.difficulty) {
                        Difficulty.EASY -> Color(0xFF10B981).copy(alpha = 0.1f)
                        Difficulty.MEDIUM -> Color(0xFFF59E0B).copy(alpha = 0.1f)
                        Difficulty.HARD -> Color(0xFFEF4444).copy(alpha = 0.1f)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = when(session.note.difficulty) {
                            Difficulty.EASY -> "Fácil"
                            Difficulty.MEDIUM -> "Média"
                            Difficulty.HARD -> "Difícil"
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when(session.note.difficulty) {
                            Difficulty.EASY -> Color(0xFF10B981)
                            Difficulty.MEDIUM -> Color(0xFFF59E0B)
                            Difficulty.HARD -> Color(0xFFEF4444)
                        }
                    )
                }
            }
        }
    }

    if (isReviewing) {
        ModalBottomSheet(
            onDismissRequest = { isReviewing = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            ReviewFlow(session, viewModel) {
                isReviewing = false
            }
        }
    }
}

@Composable
fun ReviewFlow(session: StudySession, viewModel: CollegeViewModel, onFinish: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val apiKey = "AIzaSyAQMyzTBaQ8zBy7J_-sRF84zpbuPhZHyNU"
    val assistant = remember { AiAssistant(apiKey) }
    
    var reviewMaterial by remember { mutableStateOf("") }
    var quizJson by remember { mutableStateOf("") }
    val quizQuestions = remember { mutableStateListOf<QuizQuestion>() }
    val userAnswers = remember { mutableStateListOf<Int?>() }
    var isLoading by remember { mutableStateOf(false) }

    val reviewId = "${session.note.subjectId}_${session.note.title.trim().replace(" ", "_")}"

    LaunchedEffect(Unit) {
        isLoading = true
        viewModel.getGeneratedReview(reviewId) { saved ->
            if (saved != null) {
                reviewMaterial = saved.reviewMaterial
                quizJson = saved.quizJson
                if (quizJson.isNotBlank()) {
                    try {
                        val type = object : TypeToken<List<QuizQuestion>>() {}.type
                        val parsed: List<QuizQuestion> = Gson().fromJson(quizJson, type)
                        quizQuestions.clear()
                        quizQuestions.addAll(parsed)
                        userAnswers.clear()
                        repeat(quizQuestions.size) { userAnswers.add(null) }
                    } catch (e: Exception) {}
                }
                isLoading = false
            } else {
                scope.launch {
                    assistant.generateSubjectReviewStream(session.subject?.name ?: "Matéria", session.note.content)
                        .onCompletion {
                            viewModel.saveGeneratedReview(GeneratedReviewEntity(
                                id = reviewId,
                                subjectId = session.note.subjectId,
                                title = session.note.title,
                                reviewMaterial = reviewMaterial,
                                quizJson = "",
                                lastUpdated = LocalDate.now()
                            ))
                        }
                        .collect { chunk ->
                            isLoading = false
                            reviewMaterial += chunk
                        }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f).padding(24.dp)) {
        Text(
            if(step == 0) "Material de Revisão" else if(step == 1) "Desafio de Conhecimento" else "Resultado do Teste",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (step) {
                0 -> {
                    if (isLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                                Text(reviewMaterial, style = MaterialTheme.typography.bodyMedium, lineHeight = 24.sp)
                            }
                        }
                    }
                }
                1 -> {
                    if (isLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(16.dp))
                                Text("A IA está criando suas questões...")
                            }
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                            items(quizQuestions.size) { qIndex ->
                                val question = quizQuestions[qIndex]
                                Column {
                                    Text("${qIndex + 1}. ${question.question}", fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(12.dp))
                                    question.options.forEachIndexed { oIndex, option ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (userAnswers[qIndex] == oIndex) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                                .clickable { userAnswers[qIndex] = oIndex }
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(selected = userAnswers[qIndex] == oIndex, onClick = { userAnswers[qIndex] = oIndex })
                                            Text(option, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    val correctCount = quizQuestions.indices.count { quizQuestions[it].correctIndex == userAnswers[it] }
                    Column(modifier = Modifier.fillMaxSize()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "${(correctCount.toFloat() / quizQuestions.size.toFloat() * 100).toInt()}%",
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Você acertou $correctCount de ${quizQuestions.size} questões!",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Spacer(Modifier.height(24.dp))
                        Text("Quão difícil foi essa revisão?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Difficulty.entries.forEach { diff ->
                                Button(
                                    onClick = { viewModel.updateNoteDifficulty(session.note.subjectId, session.note.title, diff) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = when(diff) {
                                            Difficulty.EASY -> if (session.note.difficulty == diff) Color(0xFF10B981) else Color(0xFF10B981).copy(alpha = 0.6f)
                                            Difficulty.MEDIUM -> if (session.note.difficulty == diff) Color(0xFFF59E0B) else Color(0xFFF59E0B).copy(alpha = 0.6f)
                                            Difficulty.HARD -> if (session.note.difficulty == diff) Color(0xFFEF4444) else Color(0xFFEF4444).copy(alpha = 0.6f)
                                        }
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = when(diff) {
                                            Difficulty.EASY -> "Fácil"
                                            Difficulty.MEDIUM -> "Média"
                                            Difficulty.HARD -> "Difícil"
                                        },
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))
                        Text("Revisão das Questões", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.weight(1f)) {
                            items(quizQuestions.size) { index ->
                                val question = quizQuestions[index]
                                val userAnswer = userAnswers[index]
                                val isCorrect = userAnswer == question.correctIndex

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Error,
                                                contentDescription = null,
                                                tint = if (isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                "Questão ${index + 1}",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828)
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(question.question, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                        Spacer(Modifier.height(8.dp))
                                        
                                        Text(
                                            "Sua resposta: ${if (userAnswer != null) question.options[userAnswer] else "Não respondida"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828)
                                        )
                                        if (!isCorrect) {
                                            Text(
                                                "Resposta correta: ${question.options[question.correctIndex]}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF2E7D32)
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

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (step == 0) {
                    if (quizQuestions.isEmpty()) {
                        isLoading = true
                        scope.launch {
                            quizJson = assistant.generateQuiz(session.subject?.name ?: "Matéria", session.note.content)
                            try {
                                val type = object : TypeToken<List<QuizQuestion>>() {}.type
                                val parsed: List<QuizQuestion> = Gson().fromJson(quizJson, type)
                                quizQuestions.clear()
                                quizQuestions.addAll(parsed)
                                userAnswers.clear()
                                repeat(quizQuestions.size) { userAnswers.add(null) }
                                
                                viewModel.saveGeneratedReview(GeneratedReviewEntity(
                                    id = reviewId,
                                    subjectId = session.note.subjectId,
                                    title = session.note.title,
                                    reviewMaterial = reviewMaterial,
                                    quizJson = quizJson,
                                    lastUpdated = LocalDate.now()
                                ))
                                
                                step = 1
                            } catch (e: Exception) { }
                            isLoading = false
                        }
                    } else {
                        step = 1
                    }
                } else if (step == 1) {
                    if (userAnswers.all { it != null }) {
                        val correctCount = quizQuestions.indices.count { quizQuestions[it].correctIndex == userAnswers[it] }
                        val isPassed = (correctCount.toFloat() / quizQuestions.size.toFloat()) >= 0.7f
                        viewModel.updateNoteReviewAfterQuiz(session.note.subjectId, session.note.title, isPassed)
                        step = 2
                    }
                } else {
                    viewModel.deleteGeneratedReview(reviewId)
                    onFinish()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading && (step != 1 || userAnswers.all { it != null })
        ) {
            Text(if(step == 0) "Ir para o Quiz" else if(step == 1) "Ver Resultado" else "Finalizar Revisão")
        }
    }
}

@Composable
fun AcademicEventCardPremium(event: AcademicEvent) {
    val isExam = event.type == EventType.EXAM
    val accentColor = if (isExam) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isExam) Icons.Default.PriorityHigh else Icons.AutoMirrored.Filled.Assignment,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title, 
                    style = MaterialTheme.typography.titleSmall, 
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = event.description, 
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = event.date.format(DateTimeFormatter.ofPattern("dd MMM")),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Text(
                    text = if (isExam) "PROVA" else "ENTREGA",
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun RoutineTaskItemPremium(
    task: RoutineTask,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        shape = RoundedCornerShape(20.dp),
        color = if (task.isCompleted) Color.Transparent else MaterialTheme.colorScheme.surface,
        tonalElevation = if (task.isCompleted) 0.dp else 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(
                        if (task.isCompleted) MaterialTheme.colorScheme.secondary 
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                if (task.isCompleted) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = Color.White)
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            Text(
                text = task.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                color = if (task.isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
            )
            
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.DeleteOutline, 
                    null, 
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun AddTaskDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var title by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova Tarefa") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("O que você precisa fazer?") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank()) onConfirm(title) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Adicionar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int
)
