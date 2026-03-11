package com.example.collegeadmin.model

import java.time.LocalDate
import java.time.LocalTime

data class Subject(
    val id: String,
    val name: String,
    val professor: String,
    val room: String,
    val totalClasses: Int = 40,
    val absences: Int = 0,
    val p1Grade: Double? = null,
    val p1Date: LocalDate? = null,
    val p2Grade: Double? = null,
    val p2Date: LocalDate? = null,
    val pfGrade: Double? = null,
    val pfDate: LocalDate? = null,
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

    val needsPF: Boolean
        get() = p1Grade != null && p2Grade != null && (p1Grade + p2Grade) / 2.0 < 7.0

    val isPassed: Boolean
        get() {
            val gradeRequirement = if (pfGrade != null) averageGrade >= 5.0 else averageGrade >= 7.0
            return gradeRequirement && (absences <= totalClasses * 0.25)
        }

    val absencePercentage: Double
        get() = (absences.toDouble() / totalClasses.toDouble()) * 100

    val isAtRiskOfAbsence: Boolean
        get() = absencePercentage >= 20.0

    val isFailedByAbsence: Boolean
        get() = absences > totalClasses * 0.25

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
    val subjectId: String
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
    val title: String,
    val content: String,
    val date: LocalDate,
    val attachments: List<String> = emptyList()
)
