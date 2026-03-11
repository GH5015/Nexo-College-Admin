package com.example.collegeadmin.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

data class Subject(
    val id: String,
    val name: String,
    val professor: String,
    val room: String,
    val totalClasses: Int = 40,
    val absences: Int = 0,
    val p1Grade: Double? = null,
    val p1Date: LocalDate? = null,
    val p1Reminder: Boolean = false,
    val p2Grade: Double? = null,
    val p2Date: LocalDate? = null,
    val p2Reminder: Boolean = false,
    val pfGrade: Double? = null,
    val pfDate: LocalDate? = null,
    val pfReminder: Boolean = false,
    val assignments: List<Assignment> = emptyList(),
    val period: String = "2024.1"
) {
    val averageGrade: Double
        get() {
            val grades = listOfNotNull(p1Grade, p2Grade)
            val avg = if (grades.isEmpty()) 0.0 else grades.average()
            
            return if (pfGrade != null) {
                (avg + pfGrade) / 2.0
            } else {
                avg
            }
        }

    val allowedAbsences: Int
        get() = (totalClasses * 0.25).toInt()

    val remainingAbsences: Int
        get() = (allowedAbsences - absences).coerceAtLeast(0)

    val needsPF: Boolean
        get() = p1Grade != null && p2Grade != null && (p1Grade + p2Grade) / 2.0 < 7.0

    val isPassed: Boolean
        get() {
            val gradeRequirement = if (pfGrade != null) averageGrade >= 5.0 else averageGrade >= 7.0
            return gradeRequirement && (absences <= allowedAbsences)
        }

    val absencePercentage: Double
        get() = (absences.toDouble() / totalClasses.toDouble()) * 100

    val isAtRiskOfAbsence: Boolean
        get() = absences >= allowedAbsences * 0.8 // Alerta aos 80% do limite

    val isFailedByAbsence: Boolean
        get() = absences > allowedAbsences

    val passingProbability: Double
        get() {
            val target = if (pfGrade != null || needsPF) 5.0 else 7.0
            val gradeFactor = if (averageGrade >= target) 0.8 else (averageGrade / target) * 0.7
            val attendanceFactor = (1.0 - (absences.toDouble() / (totalClasses * 0.25))) * 0.2
            return ((gradeFactor + attendanceFactor.coerceAtLeast(0.0)) * 100).coerceIn(0.0, 100.0)
        }
}

data class Assignment(
    val id: String,
    val subjectId: String,
    val title: String,
    val date: LocalDate,
    val grade: Double? = null,
    val type: EventType = EventType.ASSIGNMENT
)

data class ClassSession(
    val id: String,
    val subjectId: String,
    val subjectName: String,
    val room: String,
    val dayOfWeek: Int,
    val startTime: LocalTime,
    val endTime: LocalTime
)

data class AcademicEvent(
    val id: String,
    val title: String,
    val description: String,
    val date: LocalDate,
    val type: EventType,
    val subjectId: String,
    val reminderEnabled: Boolean = false
)

enum class EventType {
    EXAM, ASSIGNMENT, OTHER
}

data class RoutineTask(
    val id: String,
    val title: String,
    val description: String,
    val isCompleted: Boolean = false
)

data class ClassNote(
    val id: String,
    val subjectId: String,
    val sessionId: String? = null,
    val title: String,
    val content: String,
    val date: LocalDate,
    val attachments: List<String> = emptyList(),
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val reviewStage: Int = 0,
    val nextReviewDate: LocalDate? = null,
    val totalReviews: Int = 0,
    val successfulReviews: Int = 0,
    val lastReviewDate: LocalDate? = null,
    val easeFactor: Double = 2.5,
    val lastInterval: Int = 0
) {
    val retentionIndex: Double
        get() {
            if (totalReviews == 0) return 0.0
            val baseReten = (successfulReviews.toDouble() / totalReviews.toDouble())
            val lastReview = lastReviewDate ?: date
            val daysSince = ChronoUnit.DAYS.between(lastReview, LocalDate.now()).coerceAtLeast(0)
            val timeWeight = Math.exp(-0.05 * daysSince) 
            return (baseReten * timeWeight * 100).coerceIn(0.0, 100.0)
        }
}

enum class Difficulty {
    EASY, MEDIUM, HARD
}

data class StudySession(
    val note: ClassNote,
    val subject: Subject?,
    val reviewDate: LocalDate
)

data class UserInfo(
    val name: String,
    val course: String,
    val shift: String,
    val currentPeriod: String,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val profilePictureUri: String? = null,
    val showHelp: Boolean = true
)

data class ChatMessage(val text: String, val isUser: Boolean)

data class AiExplanation(val point: String, val explanation: String)
