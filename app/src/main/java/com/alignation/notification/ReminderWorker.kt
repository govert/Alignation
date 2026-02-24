package com.alignation.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.alignation.AlignationApp
import com.alignation.MainActivity
import com.alignation.R
import com.alignation.data.model.EventType
import com.alignation.data.repository.AlignmentRepository
import com.alignation.data.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: AlignmentRepository,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // Check if aligners are still out
        val latestEvent = repository.getLatestEventOnce()

        if (latestEvent?.eventType == EventType.REMOVED) {
            val level = inputData.getInt(ReminderScheduler.KEY_REMINDER_LEVEL, ReminderScheduler.LEVEL_1H_WARNING)
            val settings = settingsRepository.getSettingsOnce()
            showReminderNotification(level, settings?.let { getSoundUri(level, it) })
        }

        return Result.success()
    }

    private fun getSoundUri(level: Int, settings: com.alignation.data.model.UserSettings): Uri? {
        val uriString = when (level) {
            ReminderScheduler.LEVEL_1H_WARNING -> settings.alertSound1hUri
            ReminderScheduler.LEVEL_15M_BEFORE_SOFT -> settings.alertSound15mBeforeSoftUri
            ReminderScheduler.LEVEL_15M_BEFORE_HARD -> settings.alertSound15mBeforeHardUri
            ReminderScheduler.LEVEL_5M_BEFORE_HARD -> settings.alertSound5mBeforeHardUri
            else -> null
        }
        return uriString?.let { Uri.parse(it) }
    }

    private fun showReminderNotification(level: Int, customSoundUri: Uri?) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val (title, message, priority, notificationId) = when (level) {
            ReminderScheduler.LEVEL_1H_WARNING -> NotificationConfig(
                "Aligners out for 1 hour",
                "You've been without aligners for an hour. Keep an eye on the clock.",
                NotificationCompat.PRIORITY_HIGH,
                NOTIFICATION_ID_1H
            )
            ReminderScheduler.LEVEL_15M_BEFORE_SOFT -> NotificationConfig(
                "15 min before daily target!",
                "You're approaching your daily allowance. Consider putting aligners back in.",
                NotificationCompat.PRIORITY_HIGH,
                NOTIFICATION_ID_15M_SOFT
            )
            ReminderScheduler.LEVEL_15M_BEFORE_HARD -> NotificationConfig(
                "15 min before problem day!",
                "You're close to exceeding the maximum. Put your aligners back in soon!",
                NotificationCompat.PRIORITY_MAX,
                NOTIFICATION_ID_15M_HARD
            )
            ReminderScheduler.LEVEL_5M_BEFORE_HARD -> NotificationConfig(
                "5 MINUTES until problem day!",
                "URGENT: Put your aligners back in NOW to avoid a problem day!",
                NotificationCompat.PRIORITY_MAX,
                NOTIFICATION_ID_5M_HARD
            )
            else -> NotificationConfig(
                "Aligners reminder",
                "Don't forget about your aligners!",
                NotificationCompat.PRIORITY_DEFAULT,
                NOTIFICATION_ID_1H
            )
        }

        val soundUri = customSoundUri
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        val builder = NotificationCompat.Builder(context, AlignationApp.REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(priority)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        when (level) {
            ReminderScheduler.LEVEL_1H_WARNING -> {
                builder.setSound(soundUri)
                builder.setVibrate(longArrayOf(0, 250, 250, 250))
            }
            ReminderScheduler.LEVEL_15M_BEFORE_SOFT -> {
                builder.setSound(soundUri)
                builder.setVibrate(longArrayOf(0, 500, 200, 500))
            }
            ReminderScheduler.LEVEL_15M_BEFORE_HARD -> {
                builder.setSound(soundUri)
                builder.setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
                builder.setCategory(NotificationCompat.CATEGORY_ALARM)
            }
            ReminderScheduler.LEVEL_5M_BEFORE_HARD -> {
                // Most urgent - ongoing notification with alarm sound
                builder.setSound(soundUri)
                builder.setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000))
                builder.setCategory(NotificationCompat.CATEGORY_ALARM)
                builder.setOngoing(true) // Can't be dismissed easily
            }
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(notificationId, builder.build())
    }

    private data class NotificationConfig(
        val title: String,
        val message: String,
        val priority: Int,
        val id: Int
    )

    companion object {
        const val NOTIFICATION_ID_1H = 1001
        const val NOTIFICATION_ID_15M_SOFT = 1002
        const val NOTIFICATION_ID_15M_HARD = 1003
        const val NOTIFICATION_ID_5M_HARD = 1004
    }
}
