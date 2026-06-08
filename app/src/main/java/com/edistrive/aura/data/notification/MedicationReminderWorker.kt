package com.edistrive.aura.data.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.edistrive.aura.MainActivity
import com.edistrive.aura.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class MedicationReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val medicationId = inputData.getInt(KEY_MEDICATION_ID, -1)
        val name = inputData.getString(KEY_NAME) ?: "用药提醒"
        val dosage = inputData.getString(KEY_DOSAGE).orEmpty()
        val time = inputData.getString(KEY_TIME).orEmpty()
        if (medicationId <= 0) return Result.success()

        val context = applicationContext
        AuraNotificationChannels.ensure(context)

        val notificationId = (medicationId.toString() + time.replace(":", "")).hashCode()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "该服用 $name 了"
        val body = buildString {
            if (time.isNotBlank()) append("时间：$time")
            if (dosage.isNotBlank()) {
                if (isNotEmpty()) append(" · ")
                append("剂量：$dosage")
            }
            if (isEmpty()) append("请按时服药")
        }

        val builder = NotificationCompat.Builder(context, AuraNotificationChannels.MEDICATION_REMINDER_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return Result.success()
        }

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        return Result.success()
    }

    companion object {
        const val KEY_MEDICATION_ID = "medication_id"
        const val KEY_NAME = "medication_name"
        const val KEY_DOSAGE = "medication_dosage"
        const val KEY_TIME = "medication_time"
    }
}
