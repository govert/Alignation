package com.alignation.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.alignation.data.model.AuditLogEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {

    @Insert
    suspend fun insert(entry: AuditLogEntry)

    @Query("SELECT * FROM audit_log ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<AuditLogEntry>>

    @Query("SELECT * FROM audit_log ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEntries(limit: Int = 100): Flow<List<AuditLogEntry>>

    @Query("SELECT * FROM audit_log WHERE entityType = :entityType ORDER BY timestamp DESC")
    fun getEntriesForType(entityType: String): Flow<List<AuditLogEntry>>
}
