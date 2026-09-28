package com.example.kopilka.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    const val CHANNEL_ID = "deadline_alerts"
    private const val WORK_NAME = "deadline_check"
    private const val NOTIFY_HOUR = 10

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Напоминания о дедлайнах",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "За 30 и 10 дней до срока цели" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun scheduleDaily(context: Context) {
        val now = LocalDateTime.now()
        var first = now.toLocalDate().atTime(NOTIFY_HOUR, 0)
        if (!first.isAfter(now)) first = first.plusDays(1)
        val delayMs = Duration.between(now, first).toMillis()

        val request = PeriodicWorkRequestBuilder<DeadlineNotificationWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
        )
    }
}