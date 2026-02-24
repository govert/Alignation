package com.alignation.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey
    val id: Int = 1, // Single row table
    val treatmentStartDate: LocalDate,
    val treatmentWeeks: Int = 16,
    val dailyAllowanceMinutes: Int = 120, // 2 hours target
    val maxAllowanceMinutes: Int = 180, // 3 hours = problem day
    val maxGraceMinutes: Int = 30, // Max carry-over from previous day
    val createdAt: Instant = Instant.now(),
    // Alarm sound URIs (null = system default)
    val alertSound1hUri: String? = null,
    val alertSound15mBeforeSoftUri: String? = null,
    val alertSound15mBeforeHardUri: String? = null,
    val alertSound5mBeforeHardUri: String? = null,
    // Feature toggles
    val enableGraceTime: Boolean = true,
    // Alarm enable/disable toggles
    val enableAlarm1h: Boolean = true,
    val enableAlarm15mBeforeSoft: Boolean = true,
    val enableAlarm15mBeforeHard: Boolean = true,
    val enableAlarm5mBeforeHard: Boolean = true
)
