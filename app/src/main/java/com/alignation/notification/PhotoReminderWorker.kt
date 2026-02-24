package com.alignation.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.alignation.AlignationApp
import com.alignation.MainActivity
import com.alignation.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class PhotoReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        showPhotoReminder()
        return Result.success()
    }

    private fun showPhotoReminder() {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            PHOTO_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, AlignationApp.REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time for progress photos!")
            .setContentText("Take your weekly front, left, and right photos to track your progress.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID_PHOTO, notification)
    }

    companion object {
        const val NOTIFICATION_ID_PHOTO = 2001
        const val PHOTO_REQUEST_CODE = 200
        const val PHOTO_REMINDER_WORK = "weekly_photo_reminder"
    }
}
