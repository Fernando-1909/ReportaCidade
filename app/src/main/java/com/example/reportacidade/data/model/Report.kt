package com.example.reportacidade.data.model

data class Report(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val description: String = "",
    val address: String = "",
    val category: ReportCategory = ReportCategory.BURACO,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val imageUrls: List<String> = emptyList(),
    val status: ReportStatus = ReportStatus.PENDENTE,
    val createdAt: Long = System.currentTimeMillis(),
    val likedBy: List<String> = emptyList(),
    val likeCount: Int = 0
)
