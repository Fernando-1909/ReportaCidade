package com.example.reportacidade.data.repository

import com.example.reportacidade.data.model.Report
import com.example.reportacidade.data.model.ReportCategory
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ReportRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ReportRepository {

    private val reportsCollection = firestore.collection("reports")

    override suspend fun createReport(report: Report): Result<Unit> {
        return try {
            val docRef = reportsCollection.document()
            val reportWithId = report.copy(id = docRef.id)
            docRef.set(reportWithId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateReport(report: Report): Result<Unit> {
        return try {
            reportsCollection.document(report.id).set(report).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteReport(reportId: String): Result<Unit> {
        return try {
            reportsCollection.document(reportId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getAllReports(): Flow<List<Report>> {
        return reportsCollection
            .orderBy("likeCount", Query.Direction.DESCENDING)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Report::class.java)
            }
    }

    override fun getReportsByCategory(category: ReportCategory): Flow<List<Report>> {
        return reportsCollection
            .whereEqualTo("category", category.name)
            .orderBy("likeCount", Query.Direction.DESCENDING)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Report::class.java)
            }
    }

    override fun getReportsByUser(userId: String): Flow<List<Report>> {
        return reportsCollection
            .whereEqualTo("userId", userId)
            .orderBy("likeCount", Query.Direction.DESCENDING)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Report::class.java)
            }
    }

    override suspend fun getReportById(reportId: String): Report? {
        return try {
            val snapshot = reportsCollection.document(reportId).get().await()
            snapshot.toObject(Report::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun toggleLike(reportId: String, userId: String): Result<Unit> {
        return try {
            val docRef = reportsCollection.document(reportId)
            val snapshot = docRef.get().await()
            val report = snapshot.toObject(Report::class.java) ?: throw Exception("Relato não encontrado")
            
            val isLiked = report.likedBy.contains(userId)
            val updateLikedBy = if (isLiked) {
                FieldValue.arrayRemove(userId)
            } else {
                FieldValue.arrayUnion(userId)
            }
            
            val updateLikeCount = if (isLiked) {
                FieldValue.increment(-1)
            } else {
                FieldValue.increment(1)
            }
            
            docRef.update(
                "likedBy", updateLikedBy,
                "likeCount", updateLikeCount
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
