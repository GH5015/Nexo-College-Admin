package com.example.collegeadmin.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.collegeadmin.calendar.CalendarAccount
import com.example.collegeadmin.calendar.CalendarSyncHelper
import com.example.collegeadmin.data.CollegeRepository
import com.example.collegeadmin.model.*
import com.example.collegeadmin.data.local.AiStudySummaryEntity
import com.example.collegeadmin.data.local.GeneratedReviewEntity
import com.example.collegeadmin.data.local.AiStudyPlanEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

data class CalendarDay(
    val date: LocalDate,
    val sessions: List<ClassSession>,
    val isPast: Boolean
)

class CollegeViewModel(private val repository: CollegeRepository) : ViewModel() {
    val subjects: StateFlow<List<Subject>> = repository.subjects
    val sessions: StateFlow<List<ClassSession>> = repository.sessions
    val events: StateFlow<List<AcademicEvent>> = repository.events
    val tasks: StateFlow<List<RoutineTask>> = repository.tasks
    val notes: StateFlow<List<ClassNote>> = repository.notes
    val userInfo: StateFlow<UserInfo?> = repository.userInfo
    val allPeriods: StateFlow<List<String>> = repository.allPeriods

    // Mapa de matérias para acesso O(1) por ID
    val subjectsMap: StateFlow<Map<String, Subject>> = subjects.map { list ->
        list.associateBy { it.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    fun resetSaveSuccess() {
        _saveSuccess.value = false
    }

    // Cálculo do CR (Coeficiente de Rendimento) Global
    val cr: StateFlow<Double> = subjects.map { list ->
        if (list.isEmpty()) 0.0
        else list.map { it.averageGrade }.average()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Cálculo do Progresso do Semestre
    val semesterProgress: StateFlow<Float> = userInfo.map { info ->
        if (info == null) 0f
        else {
            val now = LocalDate.now()
            val totalDays = ChronoUnit.DAYS.between(info.periodStart, info.periodEnd).toDouble().coerceAtLeast(1.0)
            val daysPassed = ChronoUnit.DAYS.between(info.periodStart, now).toDouble().coerceIn(0.0, totalDays)
            (daysPassed / totalDays).toFloat()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    // Agrupamento de sessões por semana para o Cronograma
    val fullSchedule: StateFlow<Map<Int, List<CalendarDay>>> = combine(sessions, userInfo) { sessionList, info ->
        if (info == null) emptyMap<Int, List<CalendarDay>>()
        else {
            val scheduleMap = mutableMapOf<Int, MutableList<CalendarDay>>()
            val totalWeeks = ChronoUnit.WEEKS.between(info.periodStart, info.periodEnd).toInt()
            
            for (weekIndex in 0..totalWeeks) {
                val weekStartDate = info.periodStart.plusWeeks(weekIndex.toLong())
                if (weekStartDate.isAfter(info.periodEnd)) continue
                
                val daysInWeek = mutableListOf<CalendarDay>()
                for (dayOffset in 0..6) {
                    val currentDate = weekStartDate.plusDays(dayOffset.toLong())
                    if (currentDate.isAfter(info.periodEnd)) continue
                    
                    val daySessions = sessionList.filter { it.dayOfWeek == currentDate.dayOfWeek.value }
                        .sortedBy { it.startTime }
                    
                    if (daySessions.isNotEmpty()) {
                        daysInWeek.add(CalendarDay(currentDate, daySessions, currentDate.isBefore(LocalDate.now())))
                    }
                }
                if (daysInWeek.isNotEmpty()) {
                    scheduleMap[weekIndex + 1] = daysInWeek
                }
            }
            scheduleMap
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Sessões da semana atual
    val currentWeekSchedule: StateFlow<List<CalendarDay>> = combine(sessions, userInfo) { sessionList, info ->
        if (info == null) emptyList<CalendarDay>()
        else {
            val today = LocalDate.now()
            val monday = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
            val daysInWeek = mutableListOf<CalendarDay>()
            
            for (i in 0..5) { // Segunda a Sábado
                val currentDate = monday.plusDays(i.toLong())
                val daySessions = sessionList.filter { it.dayOfWeek == currentDate.dayOfWeek.value }
                    .sortedBy { it.startTime }
                
                if (daySessions.isNotEmpty()) {
                    daysInWeek.add(CalendarDay(currentDate, daySessions, currentDate.isBefore(today)))
                }
            }
            daysInWeek
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Lógica de Estudo (StudySessions) otimizada com subjectsMap
    val studyList: StateFlow<List<StudySession>> = combine(notes, subjectsMap) { noteList, subjectMap ->
        val today = LocalDate.now()
        noteList
            .groupBy { it.subjectId to it.title.trim() }
            .mapNotNull { (group, groupNotes) ->
                val subjectId = group.first
                val latestNote = groupNotes.maxByOrNull { it.date } ?: return@mapNotNull null
                
                val reviewDate = latestNote.nextReviewDate ?: run {
                    val daysSinceLearn = ChronoUnit.DAYS.between(latestNote.date, today)
                    when {
                        daysSinceLearn < 1 -> latestNote.date.plusDays(1)
                        daysSinceLearn < 7 -> latestNote.date.plusDays(7)
                        daysSinceLearn < 30 -> latestNote.date.plusDays(30)
                        else -> null
                    }
                }

                if (reviewDate != null) {
                    val subject = subjectMap[subjectId]
                    val combinedContent = groupNotes.sortedBy { it.date }.joinToString("\n\n---\n\n") { it.content }
                    val representativeNote = latestNote.copy(content = combinedContent)
                    StudySession(representativeNote, subject, reviewDate)
                } else null
            }.sortedBy { it.reviewDate }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val overdueStudySessions: StateFlow<List<StudySession>> = studyList.map { list ->
        val today = LocalDate.now()
        list.filter { it.reviewDate.isBefore(today) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayStudySessions: StateFlow<List<StudySession>> = studyList.map { list ->
        val today = LocalDate.now()
        list.filter { it.reviewDate.isEqual(today) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingStudySessions: StateFlow<List<StudySession>> = studyList.map { list ->
        val today = LocalDate.now()
        list.filter { it.reviewDate.isAfter(today) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedSession = MutableStateFlow<ClassSession?>(null)
    val selectedSession: StateFlow<ClassSession?> = _selectedSession.asStateFlow()

    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate: StateFlow<LocalDate?> = _selectedDate.asStateFlow()

    private val _isDarkMode = MutableStateFlow<Boolean?>(null) // null means follow system
    val isDarkMode: StateFlow<Boolean?> = _isDarkMode.asStateFlow()

    fun toggleDarkMode(enabled: Boolean?) {
        _isDarkMode.value = enabled
    }

    fun setShowHelp(show: Boolean) {
        repository.updateShowHelp(show)
    }

    fun setSelectedSession(session: ClassSession?, date: LocalDate? = null) {
        _selectedSession.value = session
        _selectedDate.value = date
    }

    fun saveUserInfo(name: String, course: String, shift: String, period: String, start: LocalDate, end: LocalDate) {
        repository.saveUserInfo(name, course, shift, period, start, end)
    }
    
    fun startNewPeriod(newPeriodName: String, start: LocalDate, end: LocalDate) {
        repository.startNewPeriod(newPeriodName, start, end)
    }

    fun updateProfilePicture(uri: String) {
        repository.updateProfilePicture(uri)
    }

    fun addSubject(
        name: String, 
        professor: String, 
        room: String, 
        totalClasses: Int,
        dayOfWeek: Int,
        startTime: LocalTime,
        endTime: LocalTime
    ) {
        repository.addSubject(name, professor, room, totalClasses, dayOfWeek, startTime, endTime)
    }

    fun addHistoricalSubject(
        name: String, 
        professor: String, 
        period: String, 
        p1: Double?, 
        p2: Double?, 
        pf: Double?, 
        absences: Int
    ) {
        repository.addHistoricalSubject(name, professor, period, p1, p2, pf, absences)
    }

    fun editSubject(id: String, name: String, professor: String, room: String, totalClasses: Int) {
        repository.editSubject(id, name, professor, room, totalClasses)
    }

    fun deleteSubject(id: String) {
        repository.deleteSubject(id)
    }

    fun addAbsence(subjectId: String) {
        repository.addAbsence(subjectId)
    }

    fun removeAbsence(subjectId: String) {
        repository.removeAbsence(subjectId)
    }

    fun updateGrades(
        subjectId: String, 
        p1: Double?, p1Date: LocalDate?, p1Reminder: Boolean,
        p2: Double?, p2Date: LocalDate?, p2Reminder: Boolean,
        pf: Double?, pfDate: LocalDate?, pfReminder: Boolean
    ) {
        repository.updateGrades(subjectId, p1, p1Date, p1Reminder, p2, p2Date, p2Reminder, pf, pfDate, pfReminder)
        _saveSuccess.value = true
    }

    fun addAssignment(subjectId: String, title: String, date: LocalDate, type: EventType) {
        repository.addAssignment(subjectId, title, date, type)
    }

    fun deleteAssignment(id: String) {
        repository.deleteAssignment(id)
    }

    fun toggleTask(taskId: String) {
        repository.toggleTask(taskId)
    }

    fun addTask(title: String, description: String) {
        repository.addTask(title, description)
    }

    fun deleteTask(taskId: String) {
        repository.deleteTask(taskId)
    }

    fun saveNote(id: String?, subjectId: String, sessionId: String? = null, title: String, content: String, date: LocalDate, attachments: List<String>, difficulty: Difficulty = Difficulty.MEDIUM) {
        repository.saveNote(id, subjectId, sessionId, title, content, date, attachments, difficulty)
    }
    
    fun deleteNote(id: String) {
        repository.deleteNote(id)
    }

    fun getAvailableCalendars(context: Context): List<CalendarAccount> {
        return CalendarSyncHelper(context).getAvailableCalendars()
    }

    fun syncToCalendar(context: Context, calendarId: Long, syncClasses: Boolean) {
        val syncHelper = CalendarSyncHelper(context)
        
        events.value.forEach { event ->
            syncHelper.syncEvent(event, calendarId)
        }
        
        if (syncClasses) {
            sessions.value.forEach { session ->
                syncHelper.syncSession(session, calendarId)
            }
        }
    }

    fun unsyncCalendar(context: Context, calendarId: Long) {
        CalendarSyncHelper(context).unsyncCalendar(calendarId)
    }
    
    private val _historySubjects = MutableStateFlow<List<Subject>>(emptyList())
    val historySubjects: StateFlow<List<Subject>> = _historySubjects.asStateFlow()
    
    fun loadHistory(period: String) {
        viewModelScope.launch {
            _historySubjects.value = repository.getHistoryForPeriod(period)
        }
    }

    // IA - Resumos de Prova
    fun getStudySummary(examId: String, onResult: (AiStudySummaryEntity?) -> Unit) {
        viewModelScope.launch {
            onResult(repository.getStudySummary(examId))
        }
    }

    fun saveStudySummary(examId: String, summary: String, explanations: List<Pair<String, String>>, userNotes: String) {
        viewModelScope.launch {
            repository.saveStudySummary(AiStudySummaryEntity(examId, summary, explanations, userNotes))
        }
    }

    // IA - Revisões Diárias
    fun getGeneratedReview(id: String, onResult: (GeneratedReviewEntity?) -> Unit) {
        viewModelScope.launch {
            onResult(repository.getGeneratedReview(id))
        }
    }

    fun saveGeneratedReview(review: GeneratedReviewEntity) {
        viewModelScope.launch {
            repository.saveGeneratedReview(review)
        }
    }

    fun deleteGeneratedReview(id: String) {
        viewModelScope.launch {
            repository.deleteGeneratedReview(id)
        }
    }

    fun updateNoteDifficulty(subjectId: String, title: String, newDifficulty: Difficulty) {
        viewModelScope.launch {
            repository.updateNoteDifficulty(subjectId, title, newDifficulty)
        }
    }

    fun updateNoteReviewAfterQuiz(subjectId: String, title: String, isCorrect: Boolean) {
        viewModelScope.launch {
            repository.updateNoteReviewAfterQuiz(subjectId, title, isCorrect)
        }
    }

    // IA - Plano de Estudo
    fun getStudyPlan(onResult: (AiStudyPlanEntity?) -> Unit) {
        viewModelScope.launch {
            onResult(repository.getStudyPlan())
        }
    }

    fun saveStudyPlan(content: String) {
        viewModelScope.launch {
            repository.saveStudyPlan(AiStudyPlanEntity(content = content, lastUpdated = LocalDate.now()))
        }
    }
}
