package com.linguaai.app.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    @SerialName("password_hash")
    val passwordHash: String? = null,
    @SerialName("native_language_id")
    val nativeLanguageId: Int? = null,
    @SerialName("target_language_id")
    val targetLanguageId: Int? = null,
    @SerialName("difficulty_level")
    val difficultyLevel: String? = null,
    @SerialName("daily_goal_minutes")
    val dailyGoalMinutes: Int? = 15,
    @SerialName("daily_goal_words")
    val dailyGoalWords: Int? = 10,
    @SerialName("streak_days")
    val streakDays: Int? = 0,
    @SerialName("longest_streak")
    val longestStreak: Int? = 0,
    @SerialName("total_xp")
    val totalXp: Int? = 0,
    @SerialName("last_activity_date")
    val lastActivityDate: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)
