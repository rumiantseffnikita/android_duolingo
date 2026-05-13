package com.linguaai.app.services

import android.util.Log
import com.linguaai.app.models.Language
import com.linguaai.app.models.User
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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
            val result = client.postgrest["languages"]
                .select()
                .decodeList<Language>()
                .filter { it.isActive != false }
                .sortedBy { it.name }
            Log.d("AuthService", "Loaded ${result.size} languages from Supabase")
            if (result.isEmpty()) getFallbackLanguages() else result
        } catch (e: Exception) {
            Log.e("AuthService", "getAllLanguages error: ${e.message}", e)
            getFallbackLanguages()
        }
    }

    private fun getFallbackLanguages(): List<Language> {
        return listOf(
            Language(1, "en", "Английский", "English", "\uD83C\uDDEC\uD83C\uDDE7", true),
            Language(2, "ru", "Русский", "Русский", "\uD83C\uDDF7\uD83C\uDDFA", true),
            Language(3, "de", "Немецкий", "Deutsch", "\uD83C\uDDE9\uD83C\uDDEA", true),
            Language(4, "fr", "Французский", "Français", "\uD83C\uDDEB\uD83C\uDDF7", true),
            Language(5, "es", "Испанский", "Español", "\uD83C\uDDEA\uD83C\uDDF8", true),
            Language(6, "it", "Итальянский", "Italiano", "\uD83C\uDDEE\uD83C\uDDF9", true),
            Language(7, "zh", "Китайский", "中文", "\uD83C\uDDE8\uD83C\uDDF3", true),
            Language(8, "ja", "Японский", "日本語", "\uD83C\uDDEF\uD83C\uDDF5", true),
            Language(9, "ko", "Корейский", "한국어", "\uD83C\uDDF0\uD83C\uDDF7", true),
            Language(10, "pt", "Португальский", "Português", "\uD83C\uDDF5\uD83C\uDDF9", true)
        )
    }

    data class UpdateResult(
        val success: Boolean,
        val error: String? = null
    )

    suspend fun updateUserLanguages(
        userId: String,
        targetLanguageId: Int?,
        nativeLanguageId: Int?,
        difficultyLevel: String? = null,
        dailyGoalWords: Int? = null,
        dailyGoalMinutes: Int? = null
    ): UpdateResult {
        return try {
            val updates = buildJsonObject {
                targetLanguageId?.let { put("target_language_id", it) }
                nativeLanguageId?.let { put("native_language_id", it) }
                difficultyLevel?.let { put("difficulty_level", it) }
                dailyGoalWords?.let { put("daily_goal_words", it) }
                dailyGoalMinutes?.let { put("daily_goal_minutes", it) }
                put("updated_at", Instant.now().toString())
            }

            Log.d("AuthService", "updateUserLanguages: userId=$userId, updates=$updates")

            client.postgrest["users"].update(updates) {
                filter { eq("id", userId) }
            }
            Log.d("AuthService", "updateUserLanguages: success")
            UpdateResult(true)
        } catch (e: Exception) {
            Log.e("AuthService", "updateUserLanguages error: ${e.message}", e)
            UpdateResult(false, e.message)
        }
    }
}
