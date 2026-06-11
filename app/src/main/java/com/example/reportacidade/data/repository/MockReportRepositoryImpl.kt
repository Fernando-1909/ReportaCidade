package com.example.reportacidade.data.repository

import android.content.Context
import com.example.reportacidade.R
import com.example.reportacidade.data.model.Report
import com.example.reportacidade.data.model.ReportCategory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class MockReportRepositoryImpl private constructor(
    private val context: Context,
    private val notificationRepository: NotificationRepository
) : ReportRepository {
    private val sharedPreferences = context.applicationContext.getSharedPreferences("reports_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val _reportsFlow = MutableStateFlow<List<Report>>(loadReports())

    private fun loadReports(): List<Report> {
        val json = sharedPreferences.getString("reports_list", null) ?: return emptyList()
        val type = object : TypeToken<List<Report>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveReports(list: List<Report>) {
        val json = gson.toJson(list)
        sharedPreferences.edit().putString("reports_list", json).commit()
    }

    override suspend fun createReport(report: Report): Result<Unit> {
        val newList = _reportsFlow.value.toMutableList()
        val reportWithId = if (report.id.isEmpty()) report.copy(id = java.util.UUID.randomUUID().toString()) else report
        newList.add(0, reportWithId)
        _reportsFlow.value = newList
        saveReports(newList)
        return Result.success(Unit)
    }

    override suspend fun updateReport(report: Report): Result<Unit> {
        val newList = _reportsFlow.value.toMutableList()
        val index = newList.indexOfFirst { it.id == report.id }
        if (index != -1) {
            val oldReport = newList[index]
            newList[index] = report
            _reportsFlow.value = newList
            saveReports(newList)

            // Notify status change
            if (oldReport.status != report.status) {
                val reportSnippet = if (report.description.length > 60) report.description.take(60) + "..." else report.description
                val notification = com.example.reportacidade.data.model.Notification(
                    userId = report.userId,
                    reportId = report.id,
                    reportTitle = "${report.category.displayName}: $reportSnippet",
                    type = com.example.reportacidade.data.model.NotificationType.STATUS_CHANGED,
                    message = context.getString(
                        R.string.notification_status_changed_msg,
                        report.status.displayName
                    )
                )
                notificationRepository.createNotification(notification)
            }
        }
        return Result.success(Unit)
    }

    override suspend fun deleteReport(reportId: String): Result<Unit> {
        val newList = _reportsFlow.value.filter { it.id != reportId }
        _reportsFlow.value = newList
        saveReports(newList)
        return Result.success(Unit)
    }

    override fun getAllReports(): Flow<List<Report>> = _reportsFlow.map { list ->
        list.sortedWith(compareByDescending<Report> { it.likeCount }.thenByDescending { it.createdAt })
    }

    override fun getReportsByCategory(category: ReportCategory): Flow<List<Report>> {
        return _reportsFlow.map { list ->
            list.filter { it.category == category }
                .sortedWith(compareByDescending<Report> { it.likeCount }.thenByDescending { it.createdAt })
        }
    }

    override fun getReportsByUser(userId: String): Flow<List<Report>> {
        return _reportsFlow.map { list ->
            list.filter { it.userId == userId }
                .sortedWith(compareByDescending<Report> { it.likeCount }.thenByDescending { it.createdAt })
        }
    }

    override suspend fun getReportById(reportId: String): Report? {
        return _reportsFlow.value.find { it.id == reportId }
    }

    override suspend fun toggleLike(reportId: String, userId: String): Result<Unit> {
        val newList = _reportsFlow.value.toMutableList()
        val index = newList.indexOfFirst { it.id == reportId }
        if (index != -1) {
            val report = newList[index]
            val currentLikes = report.likedBy.toMutableList()
            val isLiked = currentLikes.contains(userId)
            
            if (isLiked) {
                currentLikes.remove(userId)
            } else {
                currentLikes.add(userId)
            }
            
            val newLikeCount = if (isLiked) report.likeCount - 1 else report.likeCount + 1
            newList[index] = report.copy(
                likedBy = currentLikes,
                likeCount = if (newLikeCount < 0) 0 else newLikeCount
            )
            _reportsFlow.value = newList
            saveReports(newList)

            // Notify like received (if it's not the owner liking their own report)
            if (!isLiked && report.userId != userId) {
                val reportSnippet = if (report.description.length > 60) report.description.take(60) + "..." else report.description
                val notification = com.example.reportacidade.data.model.Notification(
                    userId = report.userId,
                    reportId = report.id,
                    reportTitle = "${report.category.displayName}: $reportSnippet",
                    type = com.example.reportacidade.data.model.NotificationType.LIKE_RECEIVED,
                    message = context.getString(R.string.notification_like_received_msg)
                )
                notificationRepository.createNotification(notification)
            }

            return Result.success(Unit)
        }
        return Result.failure(Exception("Relato não encontrado"))
    }

    fun clearAllReports() {
        sharedPreferences.edit().clear().commit()
        _reportsFlow.value = emptyList()
    }

    companion object {
        @Volatile
        private var INSTANCE: MockReportRepositoryImpl? = null

        fun getInstance(context: Context): MockReportRepositoryImpl {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MockReportRepositoryImpl(
                    context,
                    MockNotificationRepositoryImpl.getInstance(context)
                ).also { INSTANCE = it }
            }
        }
    }
}
