package com.alignation.notification

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val REMINDER_WORK_30_MIN = "aligner_reminder_30"
        const val REMINDER_WORK_45_MIN = "aligner_reminder_45"
        const val REMINDER_WORK_60_MIN = "aligner_reminder_60"

        const val KEY_REMINDER_LEVEL = "reminder_level"

        const val LEVEL_GENTLE = 0    // 30 min
        const val LEVEL_SERIOUS = 1   // 45 min
        const val LEVEL_ALARM = 2     // 60 min
    }

    fun scheduleTimedReminders() {
        val workManager = WorkManager.getInstance(context)

        // Schedule 30-minute gentle reminder
        val work30 = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(30, TimeUnit.MINUTES)
            .setInputData(workDataOf(KEY_REMINDER_LEVEL to LEVEL_GENTLE))
            .build()

        // Schedule 45-minute serious reminder
        val work45 = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(45, TimeUnit.MINUTES)
            .setInputData(workDataOf(KEY_REMINDER_LEVEL to LEVEL_SERIOUS))
            .build()

        // Schedule 60-minute alarm
        val work60 = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(60, TimeUnit.MINUTES)
            .setInputData(workDataOf(KEY_REMINDER_LEVEL to LEVEL_ALARM))
            .build()

        workManager.enqueueUniqueWork(REMINDER_WORK_30_MIN, ExistingWorkPolicy.REPLACE, work30)
        workManager.enqueueUniqueWork(REMINDER_WORK_45_MIN, ExistingWorkPolicy.REPLACE, work45)
        workManager.enqueueUniqueWork(REMINDER_WORK_60_MIN, ExistingWorkPolicy.REPLACE, work60)
    }

    fun cancelAllReminders() {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(REMINDER_WORK_30_MIN)
        workManager.cancelUniqueWork(REMINDER_WORK_45_MIN)
        workManager.cancelUniqueWork(REMINDER_WORK_60_MIN)
    }

    // Legacy method for compatibility
    fun scheduleReminder(delayMinutes: Long = 60) {
        scheduleTimedReminders()
    }

    fun cancelReminder() {
        cancelAllReminders()
    }
}
