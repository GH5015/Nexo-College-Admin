package com.example.collegeadmin.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.collegeadmin.model.Difficulty
import com.example.collegeadmin.model.EventType
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val professor: String,
    val room: String,
    val totalClasses: Int,
    val absences: Int,
    val p1Grade: Double?,
    val p1Date: LocalDate?,
    val p1Reminder: Boolean = false,
    val p2Grade: Double?,
    val p2Date: LocalDate?,
    val p2Reminder: Boolean = false,
    val pfGrade: Double?,
    val pfDate: LocalDate?,
    val pfReminder: Boolean = false,
    val period: String
)

@Entity(tableName = "assignments")
data class AssignmentEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val title: String,
    val date: LocalDate,
    val grade: Double?,
    val type: EventType
)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val subjectName: String,
    val room: String,
    val dayOfWeek: Int,
    val startTime: LocalTime,
    val endTime: LocalTime
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val isCompleted: Boolean
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val sessionId: String? = null,
    val title: String,
    val content: String,
    val date: LocalDate,
    val attachments: List<String>,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val reviewStage: Int = 0,
    val nextReviewDate: LocalDate? = null,
    val totalReviews: Int = 0,
    val successfulReviews: Int = 0,
    val lastReviewDate: LocalDate? = null,
    val easeFactor: Double = 2.5,
    val lastInterval: Int = 0
)

@Entity(tableName = "user_info")
data class UserInfoEntity(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val course: String,
    val shift: String,
    val currentPeriod: String,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val profilePictureUri: String? = null,
    val showHelp: Boolean = true
)

@Entity(tableName = "ai_study_summaries")
data class AiStudySummaryEntity(
    @PrimaryKey val examId: String,
    val baseSummary: String,
    val additionalExplanations: List<Pair<String, String>>,
    val userNotes: String = ""
)

@Entity(tableName = "generated_reviews")
data class GeneratedReviewEntity(
    @PrimaryKey val id: String, // subjectId_title
    val subjectId: String,
    val title: String,
    val reviewMaterial: String,
    val quizJson: String,
    val lastUpdated: LocalDate
)

@Entity(tableName = "ai_study_plans")
data class AiStudyPlanEntity(
    @PrimaryKey val id: String = "current_plan",
    val content: String,
    val lastUpdated: LocalDate
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
