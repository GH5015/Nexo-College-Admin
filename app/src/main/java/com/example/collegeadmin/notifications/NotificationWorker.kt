package com.example.collegeadmin.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.collegeadmin.data.CollegeRepository
import com.example.collegeadmin.data.local.AppDatabase
import com.example.collegeadmin.model.EventType
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class NotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = CollegeRepository(database.collegeDao())
        val helper = NotificationHelper(applicationContext)

        val events = repository.events.first()
        val sessions = repository.sessions.first()
        val notes = repository.notes.first()
        val today = LocalDate.now()
        val now = LocalTime.now()

        // 1. Notificações de Provas e Trabalhos
        events.forEach { event ->
            val daysUntil = ChronoUnit.DAYS.between(today, event.date)
            
            // Lógica padrão (7 e 3 dias)
            if (daysUntil == 7L || daysUntil == 3L) {
                val typeLabel = if (event.type == EventType.EXAM) "Prova" else "Trabalho"
                val timeLabel = if (daysUntil == 7L) "1 semana" else "3 dias"
                
                helper.showNotification(
                    title = "Lembrete: $typeLabel em $timeLabel",
                    message = "Você tem uma ${typeLabel.lowercase()} de ${event.title} no dia ${event.date}. Revise seu resumo de IA!",
                    notificationId = event.id.hashCode() + daysUntil.toInt()
                )
            }
            
            // Lógica Customizada: 2 dias antes (se habilitado pelo usuário)
            if (event.reminderEnabled && daysUntil == 2L) {
                helper.showNotification(
                    title = "Atenção: Prova em 48 horas! 🎯",
                    message = "Sua prova de ${event.title} é depois de amanhã. Que tal uma sessão de estudos com o Tutor IA?",
                    notificationId = event.id.hashCode() + 200
                )
            }
        }

        // 2. Notificações de Aula (2 horas antes)
        val currentDayOfWeek = today.dayOfWeek.value
        sessions.filter { it.dayOfWeek == currentDayOfWeek }.forEach { session ->
            val secondsUntil = ChronoUnit.SECONDS.between(now, session.startTime)
            val twoHoursInSeconds = 2 * 60 * 60
            
            if (secondsUntil in (twoHoursInSeconds - 1800)..(twoHoursInSeconds + 1800)) {
                helper.showNotification(
                    title = "Aula de ${session.subjectName} em breve!",
                    message = "Sua aula começa às ${session.startTime} na sala ${session.room}. Não esqueça de anotar os pontos principais!",
                    notificationId = session.id.hashCode()
                )
            }
        }

        // 3. Notificações da Curva de Esquecimento (Urgência Saudável)
        val pendingReviews = notes.groupBy { it.subjectId to it.title.trim() }
            .mapNotNull { (group, groupNotes) ->
                val latestNote = groupNotes.maxByOrNull { it.date } ?: return@mapNotNull null
                val reviewDate = latestNote.nextReviewDate ?: latestNote.date.plusDays(1)
                
                if (reviewDate.isEqual(today) || reviewDate.isBefore(today)) latestNote else null
            }

        if (pendingReviews.isNotEmpty()) {
            val firstReview = pendingReviews.first()
            val count = pendingReviews.size
            
            val title = if (count == 1) "Não deixe o conhecimento fugir!" else "Sua memória precisa de você!"
            val message = if (count == 1) {
                "Você está prestes a esquecer \"${firstReview.title}\". Revisar agora leva só 3 minutos!"
            } else {
                "Você está prestes a esquecer \"${firstReview.title}\" e mais ${count - 1} conteúdos. 5 minutos de revisão salvam seu dia!"
            }

            helper.showNotification(
                title = title,
                message = message,
                notificationId = 999
            )
        }

        // 4. Lembrete do Plano de Estudos (Manhã)
        if (now.hour == 8) {
            val studyPlan = repository.getStudyPlan()
            if (studyPlan != null) {
                helper.showNotification(
                    title = "Seu Plano de Estudos de Hoje",
                    message = "A IA já traçou sua rota de hoje. Vamos conquistar esses objetivos?",
                    notificationId = 888
                )
            }
        }

        return Result.success()
    }
}
