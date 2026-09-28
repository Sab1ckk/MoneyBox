package com.example.kopilka

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.example.kopilka.data.database.DefaultCategories
import com.example.kopilka.data.repository.CategoryRepository
import com.example.kopilka.utils.NotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltAndroidApp
class KopilkaApplication : Application(){

    @Inject lateinit var categoryRepository: CategoryRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        NotificationScheduler.createChannel(this)
        NotificationScheduler.scheduleDaily(this)
        appScope.launch {
            // Сидируем предустановленные категории только если таблица пуста —
            // чтобы не плодить дубликаты при каждом запуске.
            val existing = categoryRepository.getAllCategories().first()
            if (existing.isEmpty()) {
                DefaultCategories.list.forEach { categoryRepository.insert(it) }
            }
        }
    }
}
