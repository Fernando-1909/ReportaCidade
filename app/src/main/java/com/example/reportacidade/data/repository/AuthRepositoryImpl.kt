package com.example.reportacidade.data.repository

import com.example.reportacidade.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {

    override suspend fun signUp(name: String, email: String, password: String, city: String, neighborhood: String): Result<User> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Falha ao criar usuário")
            
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()
            
            // Aqui você geralmente salvaria os dados adicionais (cidade, bairro) no Firestore
            // Por enquanto, vamos retornar o objeto User com os dados.
            Result.success(User(
                id = firebaseUser.uid, 
                name = name, 
                email = email,
                city = city,
                neighborhood = neighborhood
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Usuário não encontrado")
            Result.success(User(
                id = firebaseUser.uid,
                name = firebaseUser.displayName ?: "",
                email = firebaseUser.email ?: ""
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
    }

    override suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePassword(email: String, newPassword: String): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser
            if (user != null && user.email == email) {
                user.updatePassword(newPassword).await()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Usuário não autenticado ou e-mail não corresponde."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCurrentUser(): User? {
        return firebaseAuth.currentUser?.let {
            User(
                id = it.uid,
                name = it.displayName ?: "",
                email = it.email ?: "",
                profileImageUrl = it.photoUrl?.toString()
            )
        }
    }

    override suspend fun updateUserProfile(user: User): Result<Unit> {
        return try {
            val firebaseUser = firebaseAuth.currentUser ?: throw Exception("Nenhum usuário logado")
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(user.name)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
