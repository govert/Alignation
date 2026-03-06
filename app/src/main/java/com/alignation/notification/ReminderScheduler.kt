package com.alignation.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.alignation.data.model.UserSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val REMINDER_WORK_30M_WARNING = "aligner_reminder_30m_warning"
        const val REMINDER_WORK_1H = "aligner_reminder_1h"
        const val REMINDER_WORK_15M_SOFT = "aligner_reminder_15m_soft"
        const val REMINDER_WORK_15M_HARD = "aligner_reminder_15m_hard"
        const val REMINDER_WORK_5M_HARD = "aligner_reminder_5m_hard"

        // Legacy work names for cancellation
        const val REMINDER_WORK_30_MIN = "aligner_reminder_30"
        const val REMINDER_WORK_45_MIN = "aligner_reminder_45"
        const val REMINDER_WORK_60_MIN = "aligner_reminder_60"

        const val KEY_REMINDER_LEVEL = "reminder_level"

        const val LEVEL_1H_WARNING = 0         // 1 hour out
        const val LEVEL_15M_BEFORE_SOFT = 1    // 15 min before daily target
        const val LEVEL_15M_BEFORE_HARD = 2    // 15 min before problem threshold
        const val LEVEL_5M_BEFORE_HARD = 3     // 5 min before problem threshold (urgent)
        const val LEVEL_30M_WARNING = 4        // 30 min out
    }

    fun scheduleTimedReminders(settings: UserSettings? = null) {
        val workManager = WorkManager.getInstance(context)
        val dailyAllowance = settings?.dailyAllowanceMinutes ?: 120
        val maxAllowance = settings?.maxAllowanceMinutes ?: 180

        // Cancel legacy work items
        workManager.cancelUniqueWork(REMINDER_WORK_30_MIN)
        workManager.cancelUniqueWork(REMINDER_WORK_45_MIN)
        workManager.cancelUniqueWork(REMINDER_WORK_60_MIN)

        // Schedule 30 min warning
        if (settings?.enableAlarm30m != false) {
            val work30m = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(30, TimeUnit.MINUTES)
                .setInputData(workDataOf(KEY_REMINDER_LEVEL to LEVEL_30M_WARNING))
                .build()
            workManager.enqueueUniqueWork(REMINDER_WORK_30M_WARNING, ExistingWorkPolicy.REPLACE, work30m)
        }

        // Schedule 1 hour warning
        if (settings?.enableAlarm1h != false) {
            val work1h = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(60, TimeUnit.MINUTES)
                .setInputData(workDataOf(KEY_REMINDER_LEVEL to LEVEL_1H_WARNING))
                .build()
            workManager.enqueueUniqueWork(REMINDER_WORK_1H, ExistingWorkPolicy.REPLACE, work1h)
        }

        // Schedule 15 min before soft limit (daily allowance)
        if (settings?.enableAlarm15mBeforeSoft != false) {
            val delayMinutes = (dailyAllowance - 15).toLong().coerceAtLeast(60)
            val work15mSoft = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .setInputData(workDataOf(KEY_REMINDER_LEVEL to LEVEL_15M_BEFORE_SOFT))
                .build()
            workManager.enqueueUniqueWork(REMINDER_WORK_15M_SOFT, ExistingWorkPolicy.REPLACE, work15mSoft)
        }

        // Schedule 15 min before hard limit (max allowance)
        if (settings?.enableAlarm15mBeforeHard != false) {
            val delayMinutes = (maxAllowance - 15).toLong().coerceAtLeast(60)
            val work15mHard = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .setInputData(workDataOf(KEY_REMINDER_LEVEL to LEVEL_15M_BEFORE_HARD))
                .build()
            workManager.enqueueUniqueWork(REMINDER_WORK_15M_HARD, ExistingWorkPolicy.REPLACE, work15mHard)
        }

        // Schedule 5 min before hard limit (urgent alarm)
        if (settings?.enableAlarm5mBeforeHard != false) {
            val delayMinutes = (maxAllowance - 5).toLong().coerceAtLeast(60)
            val work5mHard = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .setInputData(workDataOf(KEY_REMINDER_LEVEL to LEVEL_5M_BEFORE_HARD))
                .build()
            workManager.enqueueUniqueWork(REMINDER_WORK_5M_HARD, ExistingWorkPolicy.REPLACE, work5mHard)
        }
    }

    fun cancelAllReminders() {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(REMINDER_WORK_30M_WARNING)
        workManager.cancelUniqueWork(REMINDER_WORK_1H)
        workManager.cancelUniqueWork(REMINDER_WORK_15M_SOFT)
        workManager.cancelUniqueWork(REMINDER_WORK_15M_HARD)
        workManager.cancelUniqueWork(REMINDER_WORK_5M_HARD)
        // Cancel legacy work items too
        workManager.cancelUniqueWork(REMINDER_WORK_30_MIN)
        workManager.cancelUniqueWork(REMINDER_WORK_45_MIN)
        workManager.cancelUniqueWork(REMINDER_WORK_60_MIN)
    }

    fun scheduleWeeklyPhotoReminder() {
        val workManager = WorkManager.getInstance(context)

        // Calculate delay until next Thursday at 9:00 AM
        val now = LocalDateTime.now()
        var nextThursday = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.THURSDAY))
            .with(LocalTime.of(9, 0))
        if (!nextThursday.isAfter(now)) {
            nextThursday = now.with(TemporalAdjusters.next(DayOfWeek.THURSDAY))
                .with(LocalTime.of(9, 0))
        }
        val initialDelay = Duration.between(now, nextThursday)

        val photoWork = PeriodicWorkRequestBuilder<PhotoReminderWorker>(
            7, TimeUnit.DAYS
        )
            .setInitialDelay(initialDelay.toMinutes(), TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            PhotoReminderWorker.PHOTO_REMINDER_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            photoWork
        )
    }

    // Legacy methods for compatibility
    fun scheduleReminder(delayMinutes: Long = 60) {
        scheduleTimedReminders()
    }

    fun cancelReminder() {
        cancelAllReminders()
    }
}
