package com.example.reportacidade.data.repository

import com.example.reportacidade.data.model.User

interface AuthRepository {
    suspend fun signUp(name: String, email: String, password: String, city: String, neighborhood: String): Result<User>
    suspend fun signIn(email: String, password: String): Result<User>
    suspend fun signOut()
    suspend fun resetPassword(email: String): Result<Unit>
    suspend fun updatePassword(email: String, newPassword: String): Result<Unit>
    fun getCurrentUser(): User?
    suspend fun updateUserProfile(user: User): Result<Unit>
}
