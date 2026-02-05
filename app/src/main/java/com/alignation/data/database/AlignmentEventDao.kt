package com.alignation.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.alignation.data.model.AlignmentEvent
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface AlignmentEventDao {

    @Insert
    suspend fun insert(event: AlignmentEvent): Long

    @Update
    suspend fun update(event: AlignmentEvent)

    @Delete
    suspend fun delete(event: AlignmentEvent)

    @Query("SELECT * FROM alignment_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<AlignmentEvent>>

    @Query("SELECT * FROM alignment_events ORDER BY timestamp DESC LIMIT 1")
    fun getLatestEvent(): Flow<AlignmentEvent?>

    @Query("SELECT * FROM alignment_events ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestEventOnce(): AlignmentEvent?

    @Query("SELECT * FROM alignment_events WHERE timestamp >= :startOfDay AND timestamp < :endOfDay ORDER BY timestamp ASC")
    fun getEventsForDay(startOfDay: Instant, endOfDay: Instant): Flow<List<AlignmentEvent>>

    @Query("SELECT * FROM alignment_events WHERE timestamp >= :startOfDay AND timestamp < :endOfDay ORDER BY timestamp ASC")
    suspend fun getEventsForDayOnce(startOfDay: Instant, endOfDay: Instant): List<AlignmentEvent>

    @Query("SELECT * FROM alignment_events WHERE timestamp >= :startTime ORDER BY timestamp ASC")
    fun getEventsSince(startTime: Instant): Flow<List<AlignmentEvent>>

    @Query("SELECT * FROM alignment_events WHERE timestamp >= :startTime ORDER BY timestamp ASC")
    suspend fun getEventsSinceOnce(startTime: Instant): List<AlignmentEvent>

    @Query("SELECT * FROM alignment_events WHERE id = :id")
    suspend fun getEventById(id: Long): AlignmentEvent?
}
