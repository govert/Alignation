package com.alignation.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

enum class AuditAction {
    CREATE, UPDATE, DELETE
}

@Entity(tableName = "audit_log")
data class AuditLogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Instant,
    val action: AuditAction,
    val entityType: String,
    val entityId: Long,
    val oldValue: String? = null,
    val newValue: String? = null
)
