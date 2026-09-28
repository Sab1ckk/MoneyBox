package com.example.kopilka.utils

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.kopilka.MainActivity
import com.example.kopilka.data.datastore.SettingsDataStore
import com.example.kopilka.data.repository.DepositRepository
import com.example.kopilka.data.repository.GoalRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class DeadlineNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun goalRepository(): GoalRepository
        fun depositRepository(): DepositRepository
        fun settingsDataStore(): SettingsDataStore
    }

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(applicationContext, Deps::class.java)
        val settings = deps.settingsDataStore()

        val enabled = settings.notificationsEnabled.first() && settings.deadlineAlertsEnabled.first()
        val manager = NotificationManagerCompat.from(applicationContext)
        if (!enabled || !manager.areNotificationsEnabled()) return Result.success()

        val goals = deps.goalRepository().getAllOnce()
        val savedByGoal = deps.depositRepository().getAllOnce()
            .groupBy { it.goalId }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        // Чистим отметки для удалённых целей и изменённых дедлайнов
        val validPrefixes = goals.mapNotNull { g -> g.deadline?.let { "${g.id}:${it.toEpochDay()}:" } }
        val sent = settings.sentDeadlineNotifications.first()
            .filter { key -> validPrefixes.any { key.startsWith(it) } }
            .toMutableSet()

        val today = LocalDate.now()
        val openApp = PendingIntent.getActivity(
            applicationContext, 0,
            Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        for (goal in goals) {
            val deadline = goal.deadline ?: continue
            val saved = savedByGoal[goal.id] ?: 0.0
            if (saved >= goal.targetAmount) continue

            val daysLeft = ChronoUnit.DAYS.between(today, deadline)
            if (daysLeft < 0) continue

            // Этап определяем по диапазону, а не по точному числу дней —
            // так пропущенный запуск воркера (Doze) ничего не ломает.
            val stage = when {
                daysLeft <= 10 -> 10
                daysLeft <= 30 -> 30
                else -> continue
            }
            val key = "${goal.id}:${deadline.toEpochDay()}:$stage"
            if (key in sent) continue

            val remaining = formatFullAmount(goal.targetAmount - saved)
            val text = if (daysLeft == 0L) {
                "Сегодня срок цели «${goal.name}». Осталось накопить $remaining"
            } else {
                "До срока цели «${goal.name}» осталось $daysLeft дн. Не хватает $remaining"
            }

            val notification = NotificationCompat.Builder(applicationContext, NotificationScheduler.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder) // TODO: своя иконка
                .setContentTitle("Приближается дедлайн")
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(openApp)
                .setAutoCancel(true)
                .build()

            manager.notify(key.hashCode(), notification)
            sent += key
        }

        settings.setSentDeadlineNotifications(sent)
        return Result.success()
    }
}