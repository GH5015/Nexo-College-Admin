package com.example.collegeadmin.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.collegeadmin.AppDestinations
import com.example.collegeadmin.LocalIndicatorClick
import com.example.collegeadmin.model.*
import com.example.collegeadmin.ui.CollegeViewModel
import com.example.collegeadmin.ui.components.ScreenIndicator
import com.example.collegeadmin.ui.components.HelpPopup
import com.example.collegeadmin.ui.components.HelpItem
import com.example.collegeadmin.ui.theme.WarningAmber
import com.example.collegeadmin.ui.theme.PurpleAcent
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

data class PriorityItem(
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    val destination: AppDestinations,
    val buttonText: String = "Ver detalhes"
)

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
    val notes by viewModel.notes.collectAsState()
    val semesterProgress by viewModel.semesterProgress.collectAsState()
    val onMenuClick = LocalIndicatorClick.current
    
    var showHelp by remember { mutableStateOf(false) }
    
    LaunchedEffect(userInfo?.showHelp) {
        if (userInfo?.showHelp == true) {
            showHelp = true
        }
    }

    val now = LocalDate.now()
    val currentTime = LocalTime.now()

    val greeting = remember(currentTime.hour) {
        when (currentTime.hour) {
            in 5..11 -> "Bom dia"
            in 12..17 -> "Boa tarde"
            else -> "Boa noite"
        }
    }
    
    val dateString = remember(now) {
        now.format(DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM", Locale.forLanguageTag("pt-BR")))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.forLanguageTag("pt-BR")) else it.toString() }
    }

    val urgentReviews by remember {
        derivedStateOf {
            notes.filter { 
                val reviewDate = it.nextReviewDate ?: it.date.plusDays(1)
                reviewDate.isBefore(now.plusDays(1)) || it.retentionIndex < 40
            }.sortedBy { it.retentionIndex }
        }
    }

    val nextExam by remember {
        derivedStateOf {
            events.filter { 
                it.type == EventType.EXAM && !it.date.isBefore(now) 
            }.minByOrNull { it.date }
        }
    }

    val todaySessions by remember {
        derivedStateOf {
            sessions.filter { it.dayOfWeek == now.dayOfWeek.value }
                .sortedBy { it.startTime }
        }
    }
    
    val nextSession by remember {
        derivedStateOf {
            todaySessions.find { it.startTime.isAfter(currentTime) }
        }
    }

    val statusData by remember {
        derivedStateOf {
            val avgProb = if(subjects.isNotEmpty()) subjects.sumOf { it.passingProbability } / subjects.size else 100.0
            when {
                avgProb >= 70 -> "Estável" to Color(0xFF10B981) // Secondary Emerald
                avgProb >= 40 -> "Atenção" to WarningAmber
                else -> Color(0xFFF43F5E).let { "Risco" to it } // Error Rose
            }
        }
    }
    val (statusLabel, statusColor) = statusData

    if (showHelp) {
        HelpPopup(
            title = "Painel Inicial",
            helpItems = listOf(
                HelpItem(
                    "Status Acadêmico",
                    "Acompanhe sua saúde escolar baseada em notas e faltas. Mantenha a bolinha verde!",
                    Icons.Default.Analytics,
                    MaterialTheme.colorScheme.secondary
                ),
                HelpItem(
                    "Card de Prioridade",
                    "O card principal muda sozinho para te avisar o que é mais importante agora: uma prova próxima, uma revisão atrasada ou sua próxima aula.",
                    Icons.Default.Star,
                    WarningAmber
                ),
                HelpItem(
                    "Agenda de Hoje",
                    "Veja rapidamente quais matérias você terá hoje e os horários das aulas.",
                    Icons.Default.CalendarToday,
                    MaterialTheme.colorScheme.primary
                )
            ),
            onDismiss = { 
                showHelp = false
                viewModel.setShowHelp(false)
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(paddingValues)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column(Modifier.padding(top = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ScreenIndicator(label = "Início", onClick = onMenuClick)
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { showHelp = true }) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, "Ajuda", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "$greeting, ${userInfo?.name?.substringBefore(" ") ?: "Estudante"}!", 
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = dateString,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                            Spacer(Modifier.width(6.6.dp))
                            Text(
                                text = "Status acadêmico: $statusLabel",
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier.size(50.dp).clickable { onNavigate(AppDestinations.PROFILE) },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        if (userInfo?.profilePictureUri != null) {
                            AsyncImage(
                                model = userInfo?.profilePictureUri, 
                                contentDescription = "Perfil", 
                                modifier = Modifier.fillMaxSize().clip(CircleShape), 
                                contentScale = ContentScale.Crop,
                                placeholder = ColorPainter(MaterialTheme.colorScheme.primaryContainer),
                                error = rememberVectorPainter(Icons.Default.Person)
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) }
                        }
                    }
                }
            }
        }

        item {
            PriorityPager(
                urgentReviews = urgentReviews,
                nextExam = nextExam,
                nextSession = nextSession,
                onNavigate = onNavigate
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallStatCard(Modifier.weight(1f), "Aulas hoje", todaySessions.size.toString(), Icons.Default.School, MaterialTheme.colorScheme.primary)
                SmallStatCard(Modifier.weight(1f), "Trabalhos", events.count { it.type == EventType.ASSIGNMENT && !it.date.isBefore(now) }.toString(), Icons.AutoMirrored.Filled.Assignment, PurpleAcent)
                SmallStatCard(Modifier.weight(1f), "Faltas", subjects.sumOf { it.absences }.toString(), Icons.Default.Warning, WarningAmber)
            }
        }

        item {
            SectionHeader("Agenda de Hoje", onAction = { onNavigate(AppDestinations.SCHEDULE) })
            if (todaySessions.isEmpty()) {
                EmptyDashboardState("Dia livre para estudos.")
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Column(Modifier.padding(16.dp)) {
                        todaySessions.forEachIndexed { index, session ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(
                                    text = session.startTime.toString(),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(50.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Box(Modifier.width(2.dp).height(20.dp).background(MaterialTheme.colorScheme.outlineVariant))
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = session.subjectName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                            if (index < todaySessions.size - 1) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }

        if (urgentReviews.isNotEmpty()) {
            item {
                SectionHeader("🔥 Revisões Urgentes (${urgentReviews.size})", onAction = { onNavigate(AppDestinations.ROUTINE) })
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    urgentReviews.take(2).forEach { note ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.HistoryEdu, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(note.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.weight(1f))
                                Text("${note.retentionIndex.toInt()}% retenção", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(Modifier.padding(bottom = 32.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Progresso do Semestre", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("${(semesterProgress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { semesterProgress },
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Semestre ${(semesterProgress * 100).toInt()}% concluído",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun PriorityPager(
    urgentReviews: List<ClassNote>,
    nextExam: AcademicEvent?,
    nextSession: ClassSession?,
    onNavigate: (AppDestinations) -> Unit
) {
    val items by remember(urgentReviews, nextExam, nextSession) {
        derivedStateOf<List<PriorityItem>> {
            val list = mutableListOf<PriorityItem>()
            
            // Prioridade 1: Provas muito próximas (3 dias)
            nextExam?.let { exam ->
                val days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), exam.date)
                if (days <= 3) {
                    list.add(PriorityItem(
                        title = "🎯 Prova Próxima",
                        subtitle = "${exam.title} em $days dias",
                        icon = Icons.AutoMirrored.Filled.EventNote,
                        color = WarningAmber,
                        destination = AppDestinations.ROUTINE
                    ))
                }
            }

            // Prioridade 2: Revisões críticas
            urgentReviews.take(2).forEach { review ->
                list.add(PriorityItem(
                    title = "⚠ Revisão Pendente",
                    subtitle = "${review.title} • Retenção baixa",
                    icon = Icons.Default.Psychology,
                    color = Color(0xFFF43F5E), // Error Rose
                    destination = AppDestinations.ROUTINE,
                    buttonText = "Revisar agora"
                ))
            }
            
            // Prioridade 3: Próxima aula
            nextSession?.let { session ->
                list.add(PriorityItem(
                    title = "📚 Próxima Aula",
                    subtitle = "${session.subjectName}\nÀs ${session.startTime} - Sala ${session.room}",
                    icon = Icons.Default.School,
                    color = Color(0xFF6366F1), // Primary Indigo
                    destination = AppDestinations.SCHEDULE
                ))
            }
            
            if (list.isEmpty()) {
                list.add(PriorityItem(
                    title = "✅ Tudo em dia",
                    subtitle = "Nenhuma ação urgente no momento.",
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF10B981), // Secondary Emerald
                    destination = AppDestinations.DASHBOARD
                ))
            }
            list
        }
    }

    val pagerState = rememberPagerState(pageCount = { items.size })

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            pageSpacing = 16.dp
        ) { page ->
            val item = items[page]
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onNavigate(item.destination) },
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = item.color.copy(alpha = 0.1f))
            ) {
                Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.labelLarge, color = item.color, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(item.subtitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(16.dp))
                        Surface(
                            color = item.color,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = item.buttonText,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp).alpha(0.2f),
                        tint = item.color
                    )
                }
            }
        }
        
        if (items.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(items.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SmallStatCard(modifier: Modifier, label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Surface(
        modifier = modifier, 
        shape = RoundedCornerShape(20.dp), 
        color = MaterialTheme.colorScheme.surface, 
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, maxLines = 1)
        }
    }
}

@Composable
fun SectionHeader(title: String, onAction: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        TextButton(onClick = onAction) { 
            Text("Ver tudo") 
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
fun EmptyDashboardState(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(), 
        shape = RoundedCornerShape(24.dp), 
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(24.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
