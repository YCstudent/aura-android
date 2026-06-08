package com.edistrive.aura.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object AuraNotificationChannels {
    const val MEDICATION_REMINDER_ID = "medication_reminder"
    const val MEDICATION_REMINDER_NAME = "用药提醒"

    fun ensure(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(MEDICATION_REMINDER_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    MEDICATION_REMINDER_ID,
                    MEDICATION_REMINDER_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "按时服药提醒"
                    enableVibration(true)
                }
            )
        }
    }
}
