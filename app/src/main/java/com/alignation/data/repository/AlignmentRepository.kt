package com.alignation.data.repository

import com.alignation.data.database.AlignerSetDao
import com.alignation.data.database.AlignmentEventDao
import com.alignation.data.database.AuditLogDao
import com.alignation.data.model.AlignerSet
import com.alignation.data.model.AlignmentEvent
import com.alignation.data.model.AuditAction
import com.alignation.data.model.AuditLogEntry
import com.alignation.data.model.EventType
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlignmentRepository @Inject constructor(
    private val alignmentEventDao: AlignmentEventDao,
    private val alignerSetDao: AlignerSetDao,
    private val auditLogDao: AuditLogDao
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
        val id = alignmentEventDao.insert(event)
        logAudit(AuditAction.CREATE, "AlignmentEvent", id, newValue = "REMOVED at $timestamp")
        return id
    }

    suspend fun logReplacement(timestamp: Instant = Instant.now(), isManual: Boolean = false, notes: String? = null): Long {
        val event = AlignmentEvent(
            timestamp = timestamp,
            eventType = EventType.REPLACED,
            isManualEntry = isManual,
            notes = notes
        )
        val id = alignmentEventDao.insert(event)
        logAudit(AuditAction.CREATE, "AlignmentEvent", id, newValue = "REPLACED at $timestamp")
        return id
    }

    suspend fun updateEvent(event: AlignmentEvent) {
        val old = alignmentEventDao.getEventById(event.id)
        alignmentEventDao.update(event)
        logAudit(
            AuditAction.UPDATE,
            "AlignmentEvent",
            event.id,
            oldValue = old?.let { "${it.eventType} at ${it.timestamp}" },
            newValue = "${event.eventType} at ${event.timestamp}"
        )
    }

    suspend fun deleteEvent(event: AlignmentEvent) {
        alignmentEventDao.delete(event)
        logAudit(
            AuditAction.DELETE,
            "AlignmentEvent",
            event.id,
            oldValue = "${event.eventType} at ${event.timestamp}"
        )
    }

    suspend fun getEventById(id: Long): AlignmentEvent? {
        return alignmentEventDao.getEventById(id)
    }

    // Aligner Set methods
    fun getCurrentSet(): Flow<AlignerSet?> = alignerSetDao.getCurrentSet()

    suspend fun getCurrentSetOnce(): AlignerSet? = alignerSetDao.getCurrentSetOnce()

    fun getAllSets(): Flow<List<AlignerSet>> = alignerSetDao.getAllSets()

    suspend fun startNewSet(setNumber: Int, notes: String? = null): Long {
        val set = AlignerSet(
            setNumber = setNumber,
            startedAt = Instant.now(),
            notes = notes
        )
        val id = alignerSetDao.insert(set)
        logAudit(AuditAction.CREATE, "AlignerSet", id, newValue = "Set #$setNumber")
        return id
    }

    // Audit log methods
    fun getAuditLog(): Flow<List<AuditLogEntry>> = auditLogDao.getAllEntries()

    fun getRecentAuditLog(limit: Int = 100): Flow<List<AuditLogEntry>> = auditLogDao.getRecentEntries(limit)

    private suspend fun logAudit(
        action: AuditAction,
        entityType: String,
        entityId: Long,
        oldValue: String? = null,
        newValue: String? = null
    ) {
        val entry = AuditLogEntry(
            timestamp = Instant.now(),
            action = action,
            entityType = entityType,
            entityId = entityId,
            oldValue = oldValue,
            newValue = newValue
        )
        auditLogDao.insert(entry)
    }
}
