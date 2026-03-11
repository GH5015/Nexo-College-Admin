package com.example.collegeadmin.data

import com.example.collegeadmin.data.local.*
import com.example.collegeadmin.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class CollegeRepository(private val dao: CollegeDao) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _subjects = MutableStateFlow<List<Subject>>(emptyList())
    val subjects: StateFlow<List<Subject>> = _subjects.asStateFlow()

    private val _sessions = MutableStateFlow<List<ClassSession>>(emptyList())
    val sessions: StateFlow<List<ClassSession>> = _sessions.asStateFlow()

    private val _tasks = MutableStateFlow<List<RoutineTask>>(emptyList())
    val tasks: StateFlow<List<RoutineTask>> = _tasks.asStateFlow()

    private val _notes = MutableStateFlow<List<ClassNote>>(emptyList())
    val notes: StateFlow<List<ClassNote>> = _notes.asStateFlow()

    private val _events = MutableStateFlow<List<AcademicEvent>>(emptyFlowList())
    val events: StateFlow<List<AcademicEvent>> = _events.asStateFlow()

    private val _userInfo = MutableStateFlow<UserInfo?>(null)
    val userInfo: StateFlow<UserInfo?> = _userInfo.asStateFlow()
    
    private val _allPeriods = MutableStateFlow<List<String>>(emptyList())
    val allPeriods: StateFlow<List<String>> = _allPeriods.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private fun <T> emptyFlowList(): List<T> = emptyList()

    init {
        scope.launch {
            loadAllData()
        }
    }

    private suspend fun loadAllData() {
        val userEntity = dao.getUserInfo()
        val allSubjectEntities = dao.getAllSubjects()
        val sessionEntities = dao.getAllSessions()
        val taskEntities = dao.getAllTasks()
        val noteEntities = dao.getAllNotes()
        val allAssignments = dao.getAllAssignments()
        val chatEntities = dao.getAllChatMessages()

        _userInfo.value = userEntity?.let { 
            UserInfo(it.name, it.course, it.shift, it.currentPeriod, it.periodStart, it.periodEnd, it.profilePictureUri, it.showHelp)
        }

        val currentPeriod = _userInfo.value?.currentPeriod ?: "Geral"
        val periodStart = _userInfo.value?.periodStart
        val periodEnd = _userInfo.value?.periodEnd
        
        _allPeriods.value = allSubjectEntities.map { it.period }.distinct().sortedDescending()

        val filteredSubjects = allSubjectEntities.filter { it.period == currentPeriod }
        val activeSubjectIds = filteredSubjects.map { it.id }.toSet()

        _subjects.value = filteredSubjects.map { entity ->
            val assignments = allAssignments.filter { it.subjectId == entity.id }.map {
                Assignment(it.id, it.subjectId, it.title, it.date, it.grade, it.type)
            }
            
            val subjectSessions = sessionEntities.filter { it.subjectId == entity.id }
            val calculatedTotalClasses = if (periodStart != null && periodEnd != null && subjectSessions.isNotEmpty()) {
                subjectSessions.sumOf { session ->
                    countDaysInRange(periodStart, periodEnd, session.dayOfWeek)
                }
            } else {
                entity.totalClasses
            }

            Subject(
                id = entity.id,
                name = entity.name,
                professor = entity.professor,
                room = entity.room,
                totalClasses = if (calculatedTotalClasses > 0) calculatedTotalClasses else entity.totalClasses,
                absences = entity.absences,
                p1Grade = entity.p1Grade,
                p1Date = entity.p1Date,
                p1Reminder = entity.p1Reminder,
                p2Grade = entity.p2Grade,
                p2Date = entity.p2Date,
                p2Reminder = entity.p2Reminder,
                pfGrade = entity.pfGrade,
                pfDate = entity.pfDate,
                pfReminder = entity.pfReminder,
                assignments = assignments,
                period = entity.period
            )
        }

        _sessions.value = sessionEntities.filter { it.subjectId in activeSubjectIds }.map {
            ClassSession(it.id, it.subjectId, it.subjectName, it.room, it.dayOfWeek, it.startTime, it.endTime)
        }

        _tasks.value = taskEntities.map {
            RoutineTask(it.id, it.title, it.description, it.isCompleted)
        }

        _notes.value = noteEntities.filter { it.subjectId in activeSubjectIds }.map { 
            ClassNote(it.id, it.subjectId, it.sessionId, it.title, it.content, it.date, it.attachments, it.difficulty, it.reviewStage, it.nextReviewDate, it.totalReviews, it.successfulReviews, it.lastReviewDate, it.easeFactor, it.lastInterval)
        }

        _chatMessages.value = chatEntities.map { ChatMessage(it.text, it.isUser) }
        
        updateEvents()
    }

    private fun countDaysInRange(start: LocalDate, end: LocalDate, dayOfWeek: Int): Int {
        var count = 0
        var current = start
        while (!current.isAfter(end)) {
            if (current.dayOfWeek.value == dayOfWeek) {
                count++
            }
            current = current.plusDays(1)
        }
        return count
    }

    private fun updateEvents() {
        val academicEvents = mutableListOf<AcademicEvent>()
        _subjects.value.forEach { subject ->
            subject.assignments.forEach { 
                academicEvents.add(AcademicEvent(it.id, it.title, "Disciplina: ${subject.name}", it.date, it.type, subject.id))
            }
            subject.p1Date?.let { academicEvents.add(AcademicEvent("${subject.id}_p1", "P1: ${subject.name}", "Prova P1", it, EventType.EXAM, subject.id, subject.p1Reminder)) }
            subject.p2Date?.let { academicEvents.add(AcademicEvent("${subject.id}_p2", "P2: ${subject.name}", "Prova P2", it, EventType.EXAM, subject.id, subject.p2Reminder)) }
            subject.pfDate?.let { academicEvents.add(AcademicEvent("${subject.id}_pf", "PF: ${subject.name}", "Prova Final", it, EventType.EXAM, subject.id, subject.pfReminder)) }
        }
        _events.value = academicEvents.sortedBy { it.date }
    }

    fun saveUserInfo(name: String, course: String, shift: String, period: String, start: LocalDate, end: LocalDate, profilePictureUri: String? = null) {
        scope.launch {
            val currentHelp = dao.getUserInfo()?.showHelp ?: true
            dao.saveUserInfo(UserInfoEntity(1, name, course, shift, period, start, end, profilePictureUri, currentHelp))
            loadAllData()
        }
    }

    fun updateShowHelp(show: Boolean) {
        scope.launch {
            val current = dao.getUserInfo()
            if (current != null) {
                dao.saveUserInfo(current.copy(showHelp = show))
                loadAllData()
            }
        }
    }

    fun startNewPeriod(newPeriodName: String, start: LocalDate, end: LocalDate) {
        scope.launch {
            val current = dao.getUserInfo()
            if (current != null) {
                dao.saveUserInfo(current.copy(
                    currentPeriod = newPeriodName,
                    periodStart = start,
                    periodEnd = end
                ))
                val allTasks = dao.getAllTasks()
                allTasks.forEach { dao.deleteTask(it.id) }
                loadAllData()
            }
        }
    }

    fun updateProfilePicture(uri: String) {
        scope.launch {
            val current = dao.getUserInfo()
            if (current != null) {
                dao.saveUserInfo(current.copy(profilePictureUri = uri))
                loadAllData()
            }
        }
    }

    fun addSubject(name: String, professor: String, room: String, totalClasses: Int, dayOfWeek: Int, startTime: LocalTime, endTime: LocalTime) {
        scope.launch {
            val subjectId = UUID.randomUUID().toString()
            val currentPeriod = _userInfo.value?.currentPeriod ?: "Geral"
            
            val calculatedTotal = if (_userInfo.value != null) {
                countDaysInRange(_userInfo.value!!.periodStart, _userInfo.value!!.periodEnd, dayOfWeek)
            } else {
                totalClasses
            }

            val entity = SubjectEntity(
                id = subjectId,
                name = name,
                professor = professor,
                room = room,
                totalClasses = if (calculatedTotal > 0) calculatedTotal else totalClasses,
                absences = 0,
                p1Grade = null,
                p1Date = null,
                p1Reminder = false,
                p2Grade = null,
                p2Date = null,
                p2Reminder = false,
                pfGrade = null,
                pfDate = null,
                pfReminder = false,
                period = currentPeriod
            )
            dao.insertSubject(entity)
            
            val sessionId = UUID.randomUUID().toString()
            val sessionEntity = SessionEntity(sessionId, subjectId, name, room, dayOfWeek, startTime, endTime)
            dao.insertSession(sessionEntity)
            
            loadAllData()
        }
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
        scope.launch {
            val subjectId = UUID.randomUUID().toString()
            val entity = SubjectEntity(
                id = subjectId,
                name = name,
                professor = professor,
                room = "Histórico",
                totalClasses = 40,
                absences = absences,
                p1Grade = p1,
                p1Date = null,
                p1Reminder = false,
                p2Grade = p2,
                p2Date = null,
                p2Reminder = false,
                pfGrade = pf,
                pfDate = null,
                pfReminder = false,
                period = period
            )
            dao.insertSubject(entity)
            loadAllData()
        }
    }

    fun editSubject(id: String, name: String, professor: String, room: String, totalClasses: Int) {
        scope.launch {
            val current = dao.getAllSubjects().find { it.id == id }
            if (current != null) {
                val updated = current.copy(name = name, professor = professor, room = room, totalClasses = totalClasses)
                dao.insertSubject(updated)
                val sessions = dao.getAllSessions().filter { it.subjectId == id }
                sessions.forEach { 
                    dao.insertSession(it.copy(subjectName = name, room = room))
                }
                loadAllData()
            }
        }
    }

    fun deleteSubject(id: String) {
        scope.launch {
            dao.deleteSubject(id)
            dao.deleteSessionsBySubject(id)
            loadAllData()
        }
    }

    fun addAbsence(subjectId: String) {
        scope.launch {
            val currentEntity = dao.getAllSubjects().find { it.id == subjectId }
            if (currentEntity != null) {
                val updated = currentEntity.copy(absences = currentEntity.absences + 1)
                dao.insertSubject(updated)
                loadAllData()
            }
        }
    }

    fun removeAbsence(subjectId: String) {
        scope.launch {
            val currentEntity = dao.getAllSubjects().find { it.id == subjectId }
            if (currentEntity != null && currentEntity.absences > 0) {
                val updated = currentEntity.copy(absences = currentEntity.absences - 1)
                dao.insertSubject(updated)
                loadAllData()
            }
        }
    }

    fun updateGrades(
        subjectId: String, 
        p1: Double?, p1Date: LocalDate?, p1Reminder: Boolean,
        p2: Double?, p2Date: LocalDate?, p2Reminder: Boolean,
        pf: Double?, pfDate: LocalDate?, pfReminder: Boolean
    ) {
        scope.launch {
            val currentEntity = dao.getAllSubjects().find { it.id == subjectId }
            if (currentEntity != null) {
                val updated = currentEntity.copy(
                    p1Grade = p1, p1Date = p1Date, p1Reminder = p1Reminder,
                    p2Grade = p2, p2Date = p2Date, p2Reminder = p2Reminder,
                    pfGrade = pf, pfDate = pfDate, pfReminder = pfReminder
                )
                dao.insertSubject(updated)
                loadAllData()
            }
        }
    }

    fun addAssignment(subjectId: String, title: String, date: LocalDate, type: EventType) {
        scope.launch {
            val entity = AssignmentEntity(UUID.randomUUID().toString(), subjectId, title, date, null, type)
            dao.insertAssignment(entity)
            loadAllData()
        }
    }

    fun deleteAssignment(id: String) {
        scope.launch {
            dao.deleteAssignment(id)
            loadAllData()
        }
    }

    fun toggleTask(taskId: String) {
        scope.launch {
            val task = _tasks.value.find { it.id == taskId }
            if (task != null) {
                dao.insertTask(TaskEntity(task.id, task.title, task.description, !task.isCompleted))
                loadAllData()
            }
        }
    }

    fun addTask(title: String, description: String) {
        scope.launch {
            dao.insertTask(TaskEntity(UUID.randomUUID().toString(), title, description, false))
            loadAllData()
        }
    }
    
    fun deleteTask(taskId: String) {
        scope.launch {
            dao.deleteTask(taskId)
            loadAllData()
        }
    }

    fun saveNote(id: String?, subjectId: String, sessionId: String? = null, title: String, content: String, date: LocalDate = LocalDate.now(), attachments: List<String>, difficulty: Difficulty = Difficulty.MEDIUM) {
        scope.launch {
            val noteId = id ?: UUID.randomUUID().toString()
            dao.insertNote(NoteEntity(noteId, subjectId, sessionId, title, content, date, attachments, difficulty))
            loadAllData()
        }
    }

    fun deleteNote(id: String) {
        scope.launch {
            dao.deleteNote(id)
            loadAllData()
        }
    }
    
    suspend fun getHistoryForPeriod(period: String): List<Subject> {
        val allSubjects = dao.getAllSubjects().filter { it.period == period }
        val allAssignments = dao.getAllAssignments()
        
        return allSubjects.map { entity ->
            val assignments = allSubjects.filter { it.id == entity.id }.flatMap { subjectEntity -> 
                allAssignments.filter { it.subjectId == subjectEntity.id }
            }.map {
                Assignment(it.id, it.subjectId, it.title, it.date, it.grade, it.type)
            }
            Subject(
                id = entity.id,
                name = entity.name,
                professor = entity.professor,
                room = entity.room,
                totalClasses = entity.totalClasses,
                absences = entity.absences,
                p1Grade = entity.p1Grade,
                p1Date = entity.p1Date,
                p1Reminder = entity.p1Reminder,
                p2Grade = entity.p2Grade,
                p2Date = entity.p2Date,
                p2Reminder = entity.p2Reminder,
                pfGrade = entity.pfGrade,
                pfDate = entity.pfDate,
                pfReminder = entity.pfReminder,
                assignments = assignments,
                period = entity.period
            )
        }
    }

    suspend fun getStudySummary(examId: String): AiStudySummaryEntity? {
        return dao.getStudySummary(examId)
    }

    suspend fun saveStudySummary(summary: AiStudySummaryEntity) {
        dao.insertStudySummary(summary)
    }

    suspend fun getGeneratedReview(id: String): GeneratedReviewEntity? {
        return dao.getGeneratedReview(id)
    }

    suspend fun saveGeneratedReview(review: GeneratedReviewEntity) {
        dao.insertGeneratedReview(review)
    }

    suspend fun deleteGeneratedReview(id: String) {
        dao.deleteGeneratedReview(id)
    }

    suspend fun getStudyPlan(): AiStudyPlanEntity? {
        return dao.getStudyPlan()
    }

    suspend fun saveStudyPlan(plan: AiStudyPlanEntity) {
        dao.saveStudyPlan(plan)
    }

    fun updateNoteDifficulty(subjectId: String, title: String, newDifficulty: Difficulty) {
        scope.launch {
            val notesToUpdate = dao.getAllNotes().filter { it.subjectId == subjectId && it.title.trim() == title.trim() }
            notesToUpdate.forEach { note ->
                dao.insertNote(note.copy(difficulty = newDifficulty))
            }
            loadAllData()
        }
    }

    fun updateNoteReviewAfterQuiz(subjectId: String, title: String, isCorrect: Boolean) {
        scope.launch {
            val notesToUpdate = dao.getAllNotes().filter { it.subjectId == subjectId && it.title.trim() == title.trim() }
            
            notesToUpdate.forEach { note ->
                // Algoritmo SM-2 Simplificado
                val q = if (isCorrect) 5 else 2 // Qualidade da resposta (0-5). 5=perfeito, 2=erro fácil.
                
                var newEaseFactor = note.easeFactor + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
                if (newEaseFactor < 1.3) newEaseFactor = 1.3
                
                var newStage = if (isCorrect) note.reviewStage + 1 else 0
                
                val newInterval = when {
                    !isCorrect -> 1
                    newStage == 1 -> 1
                    newStage == 2 -> 6
                    else -> (note.lastInterval * newEaseFactor).toInt().coerceAtLeast(1)
                }

                val nextDate = LocalDate.now().plusDays(newInterval.toLong())
                
                dao.insertNote(note.copy(
                    reviewStage = newStage,
                    nextReviewDate = nextDate,
                    totalReviews = note.totalReviews + 1,
                    successfulReviews = if (isCorrect) note.successfulReviews + 1 else note.successfulReviews,
                    lastReviewDate = LocalDate.now(),
                    easeFactor = newEaseFactor,
                    lastInterval = newInterval
                ))
            }
            loadAllData()
        }
    }

    // Chat History
    fun addChatMessage(text: String, isUser: Boolean) {
        scope.launch {
            dao.insertChatMessage(ChatMessageEntity(text = text, isUser = isUser))
            loadAllData()
        }
    }

    fun clearChatHistory() {
        scope.launch {
            dao.clearChatHistory()
            loadAllData()
        }
    }
}
