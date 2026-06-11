package com.example.reportacidade.data.repository

import android.content.Context
import com.example.reportacidade.data.model.User
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MockAuthRepositoryImpl private constructor(context: Context) : AuthRepository {
    private val sharedPreferences = context.applicationContext.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private var currentUser: User? = loadCurrentUser()

    companion object {
        const val ADMIN_ID = "ADMIN_ID"
        const val ADMIN_LOGIN = "ADMIN"
        const val ADMIN_PASSWORD = "admin123"

        @Volatile
        private var INSTANCE: MockAuthRepositoryImpl? = null

        fun getInstance(context: Context): MockAuthRepositoryImpl {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MockAuthRepositoryImpl(context).also { INSTANCE = it }
            }
        }
    }

    private fun loadCurrentUser(): User? {
        val json = sharedPreferences.getString("current_user", null) ?: return null
        return try {
            gson.fromJson(json, User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private fun getUsers(): MutableList<Pair<User, String>> {
        val json = sharedPreferences.getString("users_list", null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<Pair<User, String>>>() {}.type
        return try {
            gson.fromJson(json, type) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    private fun saveUsers(users: List<Pair<User, String>>) {
        val json = gson.toJson(users)
        sharedPreferences.edit().putString("users_list", json).commit()
    }

    override suspend fun signUp(name: String, email: String, password: String, city: String, neighborhood: String): Result<User> {
        val users = getUsers()
        val trimmedEmail = email.trim()
        if (users.any { it.first.email.equals(trimmedEmail, ignoreCase = true) } || trimmedEmail.equals(ADMIN_LOGIN, ignoreCase = true)) {
            return Result.failure(Exception("Email já cadastrado ou reservado"))
        }

        val newUser = User(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            email = trimmedEmail,
            city = city,
            neighborhood = neighborhood
        )
        users.add(newUser to password)
        saveUsers(users)
        
        currentUser = newUser
        sharedPreferences.edit().putString("current_user", gson.toJson(newUser)).commit()
        
        return Result.success(newUser)
    }

    override suspend fun signIn(email: String, password: String): Result<User> {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()

        // Check for Admin (Accepts "ADMIN" or "admin@email.com")
        if ((trimmedEmail.equals(ADMIN_LOGIN, ignoreCase = true) || trimmedEmail.equals("admin@email.com", ignoreCase = true)) && trimmedPassword == ADMIN_PASSWORD) {
            val adminUser = User(
                id = ADMIN_ID,
                name = "admin",
                email = "admin@email.com",
                city = "Natal",
                neighborhood = "Barro Vermelho"
            )
            currentUser = adminUser
            sharedPreferences.edit().putString("current_user", gson.toJson(adminUser)).commit()
            return Result.success(adminUser)
        }

        val users = getUsers()
        val userPair = users.find { it.first.email.equals(trimmedEmail, ignoreCase = true) && it.second == trimmedPassword }
        
        return if (userPair != null) {
            currentUser = userPair.first
            sharedPreferences.edit().putString("current_user", gson.toJson(currentUser)).commit()
            Result.success(userPair.first)
        } else {
            Result.failure(Exception("Email ou senha incorretos"))
        }
    }

    override suspend fun signOut() {
        currentUser = null
        sharedPreferences.edit().remove("current_user").commit()
    }

    override suspend fun resetPassword(email: String): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun updatePassword(email: String, newPassword: String): Result<Unit> {
        val users = getUsers()
        val trimmedEmail = email.trim()
        val index = users.indexOfFirst { it.first.email.equals(trimmedEmail, ignoreCase = true) }
        return if (index != -1) {
            val user = users[index].first
            users[index] = user to newPassword
            saveUsers(users)
            Result.success(Unit)
        } else {
            Result.failure(Exception("Usuário não encontrado"))
        }
    }

    override fun getCurrentUser(): User? = currentUser

    override suspend fun updateUserProfile(user: User): Result<Unit> {
        currentUser = user
        sharedPreferences.edit().putString("current_user", gson.toJson(user)).commit()
        return Result.success(Unit)
    }

    fun clearAllData() {
        sharedPreferences.edit().clear().commit()
        currentUser = null
    }
}
