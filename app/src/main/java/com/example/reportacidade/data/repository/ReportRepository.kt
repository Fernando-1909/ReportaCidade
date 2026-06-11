package com.example.reportacidade.data.repository

import com.example.reportacidade.data.model.Report
import com.example.reportacidade.data.model.ReportCategory
import kotlinx.coroutines.flow.Flow

interface ReportRepository {
    suspend fun createReport(report: Report): Result<Unit>
    suspend fun updateReport(report: Report): Result<Unit>
    suspend fun deleteReport(reportId: String): Result<Unit>
    fun getAllReports(): Flow<List<Report>>
    fun getReportsByCategory(category: ReportCategory): Flow<List<Report>>
    fun getReportsByUser(userId: String): Flow<List<Report>>
    suspend fun getReportById(reportId: String): Report?
    suspend fun toggleLike(reportId: String, userId: String): Result<Unit>
}
