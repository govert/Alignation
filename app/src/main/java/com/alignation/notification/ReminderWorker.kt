package com.alignation.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.alignation.AlignationApp
import com.alignation.MainActivity
import com.alignation.R
import com.alignation.data.model.EventType
import com.alignation.data.repository.AlignmentRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: AlignmentRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // Check if aligners are still out
        val latestEvent = repository.getLatestEventOnce()

        if (latestEvent?.eventType == EventType.REMOVED) {
            val level = inputData.getInt(ReminderScheduler.KEY_REMINDER_LEVEL, ReminderScheduler.LEVEL_GENTLE)
            showReminderNotification(level)
        }

        return Result.success()
    }

    private fun showReminderNotification(level: Int) {
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
            ReminderScheduler.LEVEL_GENTLE -> NotificationConfig(
                "Aligners out for 30 minutes",
                "Just a gentle reminder - your aligners have been out a while.",
                NotificationCompat.PRIORITY_LOW,
                NOTIFICATION_ID_30
            )
            ReminderScheduler.LEVEL_SERIOUS -> NotificationConfig(
                "Aligners out for 45 minutes",
                "You're using up your daily budget. Consider putting them back in.",
                NotificationCompat.PRIORITY_HIGH,
                NOTIFICATION_ID_45
            )
            else -> NotificationConfig(
                "Aligners out for 1 hour!",
                "Your aligners have been out for an hour. Time to put them back!",
                NotificationCompat.PRIORITY_MAX,
                NOTIFICATION_ID_60
            )
        }

        val builder = NotificationCompat.Builder(context, AlignationApp.REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(priority)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Add sound and vibration for higher priority
        if (level >= ReminderScheduler.LEVEL_SERIOUS) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            builder.setSound(soundUri)
            builder.setVibrate(longArrayOf(0, 250, 250, 250))
        }

        // Make alarm level more persistent
        if (level == ReminderScheduler.LEVEL_ALARM) {
            builder.setOngoing(false)  // Can still be dismissed but more noticeable
            builder.setCategory(NotificationCompat.CATEGORY_ALARM)
            builder.setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
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
        const val NOTIFICATION_ID_30 = 1001
        const val NOTIFICATION_ID_45 = 1002
        const val NOTIFICATION_ID_60 = 1003
    }
}
