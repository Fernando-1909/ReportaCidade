package com.example.reportacidade.data.model

enum class NotificationType {
    STATUS_CHANGED,
    LIKE_RECEIVED
}

data class Notification(
    val id: String = "",
    val userId: String = "", // Recipient
    val reportId: String = "",
    val reportTitle: String = "",
    val type: NotificationType = NotificationType.STATUS_CHANGED,
    val message: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
