package com.example.collegeadmin.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

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
    val onMenuClick = LocalIndicatorClick.current
    
    var showHelp by remember { mutableStateOf(false) }
    
    LaunchedEffect(userInfo?.showHelp) {
        if (userInfo?.showHelp == true) {
            showHelp = true
        }
    }

    val now = LocalDate.now()
    val currentTime = LocalTime.now()

    val greeting = when (currentTime.hour) {
        in 5..11 -> "Bom dia"
        in 12..17 -> "Boa tarde"
        else -> "Boa noite"
    }
    
    val dateString = now.format(DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM", Locale("pt", "BR")))
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pt", "BR")) else it.toString() }

    val urgentReviews = notes.filter { 
        val reviewDate = it.nextReviewDate ?: it.date.plusDays(1)
        reviewDate.isBefore(now.plusDays(1)) || it.retentionIndex < 40
    }.sortedBy { it.retentionIndex }

    val nextExam = events.filter { 
        it.type == EventType.EXAM && !it.date.isBefore(now) 
    }.minByOrNull { it.date }

    val todaySessions = sessions.filter { it.dayOfWeek == now.dayOfWeek.value }
        .sortedBy { it.startTime }
    
    val nextSession = todaySessions.find { it.startTime.isAfter(currentTime) }

    val avgProb = if(subjects.isNotEmpty()) subjects.sumOf { it.passingProbability } / subjects.size else 100.0
    val (statusLabel, statusColor) = when {
        avgProb >= 70 -> "Estável" to Color(0xFF10B981)
        avgProb >= 40 -> "Atenção" to Color(0xFFF59E0B)
        else -> "Risco" to Color(0xFFEF4444)
    }

    if (showHelp) {
        HelpPopup(
            title = "Painel Inicial",
            helpItems = listOf(
                HelpItem(
                    "Status Acadêmico",
                    "Acompanhe sua saúde escolar baseada em notas e faltas. Mantenha a bolinha verde!",
                    Icons.Default.Analytics,
                    Color(0xFF10B981)
                ),
                HelpItem(
                    "Card de Prioridade",
                    "O card principal muda sozinho para te avisar o que é mais importante agora: uma prova próxima, uma revisão atrasada ou sua próxima aula.",
                    Icons.Default.Star,
                    Color(0xFFF59E0B)
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
                        Icon(Icons.Default.HelpOutline, "Ajuda", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
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
                            AsyncImage(model = userInfo?.profilePictureUri, contentDescription = "Perfil", modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                        } else {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) }
                        }
                    }
                }
            }
        }

        item {
            PriorityCard(
                urgentReview = urgentReviews.firstOrNull(),
                nextExam = nextExam,
                nextSession = nextSession,
                onNavigate = onNavigate
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallStatCard(Modifier.weight(1f), "Aulas hoje", todaySessions.size.toString(), Icons.Default.School, MaterialTheme.colorScheme.primary)
                SmallStatCard(Modifier.weight(1f), "Trabalhos", events.count { it.type == EventType.ASSIGNMENT && !it.date.isBefore(now) }.toString(), Icons.Default.Assignment, Color(0xFF8B5CF6))
                SmallStatCard(Modifier.weight(1f), "Faltas", subjects.sumOf { it.absences }.toString(), Icons.Default.Warning, Color(0xFFF59E0B))
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
            userInfo?.let { info ->
                val totalDays = java.time.temporal.ChronoUnit.DAYS.between(info.periodStart, info.periodEnd).toDouble().coerceAtLeast(1.0)
                val daysPassed = java.time.temporal.ChronoUnit.DAYS.between(info.periodStart, now).toDouble().coerceIn(0.0, totalDays)
                val progress = (daysPassed / totalDays).toFloat()
                
                Column(Modifier.padding(bottom = 32.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Progresso do Semestre", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Semestre ${(progress * 100).toInt()}% concluído",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
fun PriorityCard(
    urgentReview: ClassNote?,
    nextExam: AcademicEvent?,
    nextSession: ClassSession?,
    onNavigate: (AppDestinations) -> Unit
) {
    val (title, sub, icon, color, dest) = when {
        urgentReview != null -> listOf(
            "⚠ Revisão Pendente",
            "${urgentReview.title} • Retenção baixa",
            Icons.Default.Psychology,
            Color(0xFFEF4444),
            AppDestinations.ROUTINE
        )
        nextExam != null && java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), nextExam.date) <= 3 -> listOf(
            "🎯 Prova Próxima",
            "${nextExam.title} em ${java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), nextExam.date)} dias",
            Icons.Default.EventNote,
            Color(0xFFF59E0B),
            AppDestinations.ROUTINE
        )
        nextSession != null -> listOf(
            "📚 Próxima Aula",
            "${nextSession.subjectName}\nÀs ${nextSession.startTime} - Sala ${nextSession.room}",
            Icons.Default.School,
            Color(0xFF6366F1),
            AppDestinations.SCHEDULE
        )
        else -> listOf(
            "✅ Tudo em dia",
            "Nenhuma ação urgente no momento.",
            Icons.Default.CheckCircle,
            Color(0xFF10B981),
            AppDestinations.DASHBOARD
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onNavigate(dest as AppDestinations) },
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = (color as Color).copy(alpha = 0.1f))
    ) {
        Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title as String, style = MaterialTheme.typography.labelLarge, color = color, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(sub as String, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(16.dp))
                Surface(
                    color = color,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (dest == AppDestinations.ROUTINE) "Revisar agora" else "Ver detalhes",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Icon(
                imageVector = icon as androidx.compose.ui.graphics.vector.ImageVector,
                contentDescription = null,
                modifier = Modifier.size(64.dp).alpha(0.2f),
                tint = color
            )
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
