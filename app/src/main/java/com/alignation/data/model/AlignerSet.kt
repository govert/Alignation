package com.alignation.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "aligner_sets")
data class AlignerSet(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val setNumber: Int,
    val startedAt: Instant,
    val notes: String? = null
)
