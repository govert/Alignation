package com.alignation.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "alignment_events")
data class AlignmentEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Instant,
    val eventType: EventType,
    val isManualEntry: Boolean = false,
    val notes: String? = null
)
