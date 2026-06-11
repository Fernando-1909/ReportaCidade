package com.example.reportacidade.data.repository

import android.content.Context
import com.example.reportacidade.data.model.Notification
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class MockNotificationRepositoryImpl private constructor(context: Context) : NotificationRepository {
    private val sharedPreferences = context.applicationContext.getSharedPreferences("notifications_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val _notificationsFlow = MutableStateFlow<List<Notification>>(loadNotifications())

    private fun loadNotifications(): List<Notification> {
        val json = sharedPreferences.getString("notifications_list", null) ?: return emptyList()
        val type = object : TypeToken<List<Notification>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveNotifications(list: List<Notification>) {
        val json = gson.toJson(list)
        sharedPreferences.edit().putString("notifications_list", json).apply()
    }

    override suspend fun createNotification(notification: Notification): Result<Unit> {
        val newList = _notificationsFlow.value.toMutableList()
        val notificationWithId = if (notification.id.isEmpty()) {
            notification.copy(id = java.util.UUID.randomUUID().toString())
        } else {
            notification
        }
        newList.add(0, notificationWithId)
        _notificationsFlow.value = newList
        saveNotifications(newList)
        return Result.success(Unit)
    }

    override fun getNotificationsByUser(userId: String): Flow<List<Notification>> {
        return _notificationsFlow.map { list ->
            list.filter { it.userId == userId }
                .sortedByDescending { it.createdAt }
        }
    }

    override suspend fun markAsRead(notificationId: String): Result<Unit> {
        val newList = _notificationsFlow.value.toMutableList()
        val index = newList.indexOfFirst { it.id == notificationId }
        if (index != -1) {
            newList[index] = newList[index].copy(isRead = true)
            _notificationsFlow.value = newList
            saveNotifications(newList)
        }
        return Result.success(Unit)
    }

    override suspend fun markAllAsRead(userId: String): Result<Unit> {
        val newList = _notificationsFlow.value.map {
            if (it.userId == userId) it.copy(isRead = true) else it
        }
        _notificationsFlow.value = newList
        saveNotifications(newList)
        return Result.success(Unit)
    }

    override suspend fun deleteNotification(notificationId: String): Result<Unit> {
        val newList = _notificationsFlow.value.filter { it.id != notificationId }
        _notificationsFlow.value = newList
        saveNotifications(newList)
        return Result.success(Unit)
    }

    companion object {
        @Volatile
        private var INSTANCE: MockNotificationRepositoryImpl? = null

        fun getInstance(context: Context): MockNotificationRepositoryImpl {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MockNotificationRepositoryImpl(context).also { INSTANCE = it }
            }
        }
    }
}
