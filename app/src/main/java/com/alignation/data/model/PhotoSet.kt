package com.alignation.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "photo_sets")
data class PhotoSet(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val takenAt: Instant,
    val alignerSetNumber: Int,
    val frontPhotoPath: String? = null,
    val leftPhotoPath: String? = null,
    val rightPhotoPath: String? = null,
    val notes: String? = null
)
