package com.edistrive.aura.data.notification

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.edistrive.aura.util.DateFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicationReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun schedule(
        medicationId: Int,
        medicationName: String,
        dosage: String?,
        reminderTimes: List<String>,
        startIso: String?,
        endIso: String?,
        daysWindow: Int = 7
    ) {
        cancel(medicationId)
        if (reminderTimes.isEmpty()) return

        val today = LocalDate.now()
        val start = DateFormat.parseIsoDate(startIso)?.coerceAtLeastDate(today) ?: today
        val end = DateFormat.parseIsoDate(endIso) ?: today.plusDays(daysWindow.toLong())
        val rangeEnd = if (end.isBefore(start)) start else end
        val cap = minOf(rangeEnd, today.plusDays(daysWindow.toLong()))

        val workManager = WorkManager.getInstance(context)
        var day = start
        while (!day.isAfter(cap)) {
            reminderTimes.forEach { rawTime ->
                val time = parseTime(rawTime) ?: return@forEach
                val target = LocalDateTime.of(day, time)
                val now = LocalDateTime.now()
                if (target.isBefore(now)) return@forEach
                val delay = Duration.between(now, target).toMillis()
                val data = Data.Builder()
                    .putInt(MedicationReminderWorker.KEY_MEDICATION_ID, medicationId)
                    .putString(MedicationReminderWorker.KEY_NAME, medicationName)
                    .putString(MedicationReminderWorker.KEY_DOSAGE, dosage.orEmpty())
                    .putString(MedicationReminderWorker.KEY_TIME, rawTime)
                    .build()
                val request = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
                    .setInitialDelay(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
                    .setInputData(data)
                    .addTag(tagFor(medicationId))
                    .build()
                workManager.enqueueUniqueWork(
                    uniqueWorkName(medicationId, day, rawTime),
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            }
            day = day.plusDays(1)
        }
    }

    fun cancel(medicationId: Int) {
        WorkManager.getInstance(context).cancelAllWorkByTag(tagFor(medicationId))
    }

    private fun parseTime(raw: String): LocalTime? {
        val trimmed = raw.trim()
        return try {
            LocalTime.parse(trimmed)
        } catch (e: Exception) {
            try {
                val parts = trimmed.split(":")
                LocalTime.of(parts[0].toInt(), parts.getOrNull(1)?.toInt() ?: 0)
            } catch (e2: Exception) {
                null
            }
        }
    }

    private fun tagFor(medicationId: Int) = "medication_reminder_$medicationId"

    private fun uniqueWorkName(medicationId: Int, day: LocalDate, time: String) =
        "medication_${medicationId}_${day}_${time.replace(":", "")}"

    private fun LocalDate.coerceAtLeastDate(other: LocalDate): LocalDate =
        if (isBefore(other)) other else this
}
