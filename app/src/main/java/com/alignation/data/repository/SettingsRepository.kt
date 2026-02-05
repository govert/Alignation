package com.alignation.data.repository

import com.alignation.data.database.UserSettingsDao
import com.alignation.data.model.UserSettings
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val userSettingsDao: UserSettingsDao
) {
    fun getSettings(): Flow<UserSettings?> = userSettingsDao.getSettings()

    suspend fun getSettingsOnce(): UserSettings? = userSettingsDao.getSettingsOnce()

    suspend fun initializeSettings(startDate: LocalDate) {
        val settings = UserSettings(
            treatmentStartDate = startDate,
            createdAt = Instant.now()
        )
        userSettingsDao.insertOrUpdate(settings)
    }

    suspend fun updateStartDate(startDate: LocalDate) {
        val existing = userSettingsDao.getSettingsOnce()
        if (existing != null) {
            userSettingsDao.insertOrUpdate(existing.copy(treatmentStartDate = startDate))
        } else {
            initializeSettings(startDate)
        }
    }

    suspend fun updateSettings(settings: UserSettings) {
        userSettingsDao.insertOrUpdate(settings)
    }
}
