package com.example.collegeadmin.calendar

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast
import com.example.collegeadmin.model.AcademicEvent
import com.example.collegeadmin.model.ClassSession
import java.time.ZoneId
import java.util.*

data class CalendarAccount(val id: Long, val name: String, val email: String)

class CalendarSyncHelper(private val context: Context) {

    private val APP_TAG = "[CollegeAdmin]"

    fun getAvailableCalendars(): List<CalendarAccount> {
        val calendars = mutableListOf<CalendarAccount>()
        try {
            val projection = arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.ACCOUNT_NAME
            )
            val cursor = context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null
            )
            cursor?.use {
                while (it.moveToNext()) {
                    val id = it.getLong(0)
                    val name = it.getString(1)
                    val email = it.getString(2)
                    if (email.contains("@gmail.com")) {
                        calendars.add(CalendarAccount(id, name, email))
                    }
                }
            }
        } catch (e: Exception) { }
        return calendars
    }

    fun syncEvent(event: AcademicEvent, calendarId: Long) {
        try {
            val startMillis: Long = event.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis: Long = event.date.atTime(23, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.TITLE, "$APP_TAG ${event.title}")
                put(CalendarContract.Events.DESCRIPTION, event.description)
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                put(CalendarContract.Events.HAS_ALARM, 1)
            }

            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            
            uri?.lastPathSegment?.toLongOrNull()?.let { eventId ->
                val reminderValues = ContentValues().apply {
                    put(CalendarContract.Reminders.MINUTES, 24 * 60)
                    put(CalendarContract.Reminders.EVENT_ID, eventId)
                    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                }
                context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
            }
        } catch (e: Exception) {
            showToast("Erro ao sincronizar '${event.title}': ${e.localizedMessage}")
        }
    }

    fun syncSession(session: ClassSession, calendarId: Long) {
        try {
            val today = java.time.LocalDate.now()
            var nextSessionDate = today
            while (nextSessionDate.dayOfWeek.value != session.dayOfWeek) {
                nextSessionDate = nextSessionDate.plusDays(1)
            }

            val startDateTime = nextSessionDate.atTime(session.startTime)
            val endDateTime = nextSessionDate.atTime(session.endTime)
            
            val startMillis = startDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis = endDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.TITLE, "$APP_TAG Aula: ${session.subjectName}")
                put(CalendarContract.Events.EVENT_LOCATION, "Sala: ${session.room}")
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                put(CalendarContract.Events.RRULE, "FREQ=WEEKLY;BYDAY=${getDayOfWeekString(session.dayOfWeek)}")
            }

            context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        } catch (e: Exception) { }
    }

    fun unsyncCalendar(calendarId: Long) {
        try {
            val where = "${CalendarContract.Events.CALENDAR_ID} = ? AND ${CalendarContract.Events.TITLE} LIKE ?"
            val args = arrayOf(calendarId.toString(), "$APP_TAG%")
            val rowsDeleted = context.contentResolver.delete(CalendarContract.Events.CONTENT_URI, where, args)
            showToast("Agenda dessincronizada: $rowsDeleted eventos removidos.")
        } catch (e: Exception) {
            showToast("Erro ao dessincronizar: ${e.localizedMessage}")
        }
    }

    private fun getDayOfWeekString(day: Int): String {
        return when (day) {
            1 -> "MO"
            2 -> "TU"
            3 -> "WE"
            4 -> "TH"
            5 -> "FR"
            6 -> "SA"
            7 -> "SU"
            else -> "MO"
        }
    }
    
    private fun showToast(message: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
