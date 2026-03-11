package com.example.collegeadmin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.collegeadmin.model.ClassNote
import com.example.collegeadmin.model.Difficulty
import com.example.collegeadmin.model.Subject
import com.example.collegeadmin.ui.CollegeViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun StudyTab(viewModel: CollegeViewModel) {
    val notes by viewModel.notes.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val today = LocalDate.now()

    // Lógica da Curva de Esquecimento (Ebbinghaus)
    // Intervalos de revisão: 1 dia, 7 dias, 30 dias
    val studyList = notes.mapNotNull { note ->
        val daysSinceLearn = ChronoUnit.DAYS.between(note.date, today)
        val nextReviewDate = when {
            daysSinceLearn < 1 -> note.date.plusDays(1)
            daysSinceLearn < 7 -> note.date.plusDays(7)
            daysSinceLearn < 30 -> note.date.plusDays(30)
            else -> null
        }

        if (nextReviewDate != null) {
            val subject = subjects.find { it.id == note.subjectId }
            StudySession(note, subject, nextReviewDate)
        } else null
    }.sortedBy { it.reviewDate }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Curva de Esquecimento",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Revisões estratégicas para fortalecer sua memória.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(20.dp))

        if (studyList.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nada para revisar hoje. Bom trabalho!", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(studyList) { session ->
                    StudyReviewCard(session)
                }
            }
        }
    }
}

data class StudySession(
    val note: ClassNote,
    val subject: Subject?,
    val reviewDate: LocalDate
)

@Composable
fun StudyReviewCard(session: StudySession) {
    val daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), session.reviewDate)
    val statusColor = when {
        daysUntil < 0 -> MaterialTheme.colorScheme.error
        daysUntil == 0L -> Color(0xFFF59E0B)
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (daysUntil < 0) Icons.Default.History else Icons.Default.School,
                    null,
                    tint = statusColor
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(session.note.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(session.subject?.name ?: "Matéria", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    val difficultyColor = when(session.note.difficulty) {
                        Difficulty.EASY -> Color(0xFF10B981)
                        Difficulty.MEDIUM -> Color(0xFFF59E0B)
                        Difficulty.HARD -> Color(0xFFEF4444)
                    }
                    Box(Modifier.size(8.dp).clip(CircleShape).background(difficultyColor))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        when(session.note.difficulty) {
                            Difficulty.EASY -> "Fácil"
                            Difficulty.MEDIUM -> "Médio"
                            Difficulty.HARD -> "Difícil"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (daysUntil == 0L) "HOJE" else if (daysUntil < 0) "ATRASADO" else "EM $daysUntil DIAS",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = statusColor
                )
            }
        }
    }
}
