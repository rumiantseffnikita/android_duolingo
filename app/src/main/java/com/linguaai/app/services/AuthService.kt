package com.linguaai.app.services

import com.linguaai.app.models.Language
import com.linguaai.app.models.User
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import java.time.Instant
import java.util.UUID

class AuthService {

    private val client = SupabaseClient.client

    data class AuthResult(
        val success: Boolean,
        val message: String,
        val user: User? = null
    )

    suspend fun register(name: String, email: String, password: String): AuthResult {
        if (email.isBlank()) return AuthResult(false, "Email пустой")
        if (password.isBlank()) return AuthResult(false, "Пароль пустой")

        return try {
            val existing = client.postgrest["users"]
                .select { filter { eq("email", email) } }
                .decodeList<User>()

            if (existing.isNotEmpty()) {
                return AuthResult(false, "Пользователь уже существует")
            }

            val newUser = User(
                id = UUID.randomUUID().toString(),
                name = name,
                email = email,
                passwordHash = password,
                dailyGoalWords = 10,
                dailyGoalMinutes = 15,
                totalXp = 0,
                streakDays = 0,
                createdAt = Instant.now().toString(),
                updatedAt = Instant.now().toString()
            )

            client.postgrest["users"].insert(newUser)

            AuthResult(true, "Регистрация успешна", newUser)
        } catch (e: Exception) {
            AuthResult(false, "Ошибка: ${e.message}")
        }
    }

    suspend fun registerWithLanguage(
        name: String,
        email: String,
        password: String,
        targetLanguageId: Int,
        nativeLanguageId: Int,
        difficultyLevel: String = "beginner"
    ): AuthResult {
        if (email.isBlank()) return AuthResult(false, "Email пустой")
        if (password.isBlank()) return AuthResult(false, "Пароль пустой")

        return try {
            val existing = client.postgrest["users"]
                .select { filter { eq("email", email) } }
                .decodeList<User>()

            if (existing.isNotEmpty()) {
                return AuthResult(false, "Пользователь уже существует")
            }

            val newUser = User(
                id = UUID.randomUUID().toString(),
                name = name,
                email = email,
                passwordHash = password,
                targetLanguageId = targetLanguageId,
                nativeLanguageId = nativeLanguageId,
                difficultyLevel = difficultyLevel,
                dailyGoalWords = 10,
                dailyGoalMinutes = 15,
                totalXp = 0,
                streakDays = 0,
                createdAt = Instant.now().toString(),
                updatedAt = Instant.now().toString()
            )

            client.postgrest["users"].insert(newUser)

            AuthResult(true, "Регистрация успешна", newUser)
        } catch (e: Exception) {
            AuthResult(false, "Ошибка: ${e.message}")
        }
    }

    suspend fun login(email: String, password: String): AuthResult {
        return try {
            val users = client.postgrest["users"]
                .select {
                    filter {
                        eq("email", email)
                        eq("password_hash", password)
                    }
                }
                .decodeList<User>()

            if (users.isEmpty()) {
                AuthResult(false, "Неверный логин или пароль")
            } else {
                AuthResult(true, "Успешный вход", users.first())
            }
        } catch (e: Exception) {
            AuthResult(false, "Ошибка: ${e.message}")
        }
    }

    suspend fun getAllLanguages(): List<Language> {
        return try {
            client.postgrest["languages"]
                .select {
                    filter { eq("is_active", true) }
                }
                .decodeList<Language>()
                .sortedBy { it.name }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateUserLanguages(
        userId: String,
        targetLanguageId: Int?,
        nativeLanguageId: Int?,
        difficultyLevel: String? = null
    ): Boolean {
        return try {
            val updates = mutableMapOf<String, Any>()
            targetLanguageId?.let { updates["target_language_id"] = it }
            nativeLanguageId?.let { updates["native_language_id"] = it }
            difficultyLevel?.let { updates["difficulty_level"] = it }
            updates["updated_at"] = Instant.now().toString()

            client.postgrest["users"].update(updates) {
                filter { eq("id", userId) }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
