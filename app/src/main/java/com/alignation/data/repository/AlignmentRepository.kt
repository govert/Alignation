package com.alignation.data.repository

import com.alignation.data.database.AlignmentEventDao
import com.alignation.data.model.AlignmentEvent
import com.alignation.data.model.EventType
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlignmentRepository @Inject constructor(
    private val alignmentEventDao: AlignmentEventDao
) {
    fun getAllEvents(): Flow<List<AlignmentEvent>> = alignmentEventDao.getAllEvents()

    fun getLatestEvent(): Flow<AlignmentEvent?> = alignmentEventDao.getLatestEvent()

    suspend fun getLatestEventOnce(): AlignmentEvent? = alignmentEventDao.getLatestEventOnce()

    fun getEventsForToday(): Flow<List<AlignmentEvent>> {
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        return alignmentEventDao.getEventsForDay(startOfDay, endOfDay)
    }

    fun getEventsForDate(date: LocalDate): Flow<List<AlignmentEvent>> {
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        return alignmentEventDao.getEventsForDay(startOfDay, endOfDay)
    }

    suspend fun getEventsForDateOnce(date: LocalDate): List<AlignmentEvent> {
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        return alignmentEventDao.getEventsForDayOnce(startOfDay, endOfDay)
    }

    fun getEventsForDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<AlignmentEvent>> {
        val startOfRange = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
        return alignmentEventDao.getEventsSince(startOfRange)
    }

    suspend fun getEventsForDateRangeOnce(startDate: LocalDate, endDate: LocalDate): List<AlignmentEvent> {
        val startOfRange = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
        return alignmentEventDao.getEventsSinceOnce(startOfRange)
    }

    suspend fun logRemoval(timestamp: Instant = Instant.now(), isManual: Boolean = false, notes: String? = null): Long {
        val event = AlignmentEvent(
            timestamp = timestamp,
            eventType = EventType.REMOVED,
            isManualEntry = isManual,
            notes = notes
        )
        return alignmentEventDao.insert(event)
    }

    suspend fun logReplacement(timestamp: Instant = Instant.now(), isManual: Boolean = false, notes: String? = null): Long {
        val event = AlignmentEvent(
            timestamp = timestamp,
            eventType = EventType.REPLACED,
            isManualEntry = isManual,
            notes = notes
        )
        return alignmentEventDao.insert(event)
    }

    suspend fun updateEvent(event: AlignmentEvent) {
        alignmentEventDao.update(event)
    }

    suspend fun deleteEvent(event: AlignmentEvent) {
        alignmentEventDao.delete(event)
    }

    suspend fun getEventById(id: Long): AlignmentEvent? {
        return alignmentEventDao.getEventById(id)
    }
}
