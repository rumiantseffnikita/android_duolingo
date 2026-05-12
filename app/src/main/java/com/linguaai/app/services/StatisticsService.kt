package com.linguaai.app.services

import android.util.Log
import com.linguaai.app.models.*
import io.github.jan.supabase.postgrest.postgrest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class StatisticsService {

    private val client = SupabaseClient.client
    private val tag = "StatisticsService"

    suspend fun getUserStats(userId: String): UserStats {
        return try {
            val user = client.postgrest["users"]
                .select { filter { eq("id", userId) } }
                .decodeList<User>()
                .firstOrNull()

            val sessions = client.postgrest["learning_sessions"]
                .select {
                    filter {
                        eq("user_id", userId)
                        neq("finished_at", "null")
                    }
                    limit(20)
                }
                .decodeList<LearningSession>()

            val totalWords = client.postgrest["word_progresses"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<WordProgress>()
                .size

            val totalCorrect = sessions.sumOf { it.correctAnswers ?: 0 }
            val totalWrong = sessions.sumOf { it.wrongAnswers ?: 0 }
            val totalAnswered = totalCorrect + totalWrong

            UserStats(
                totalXp = user?.totalXp ?: 0,
                streakDays = user?.streakDays ?: 0,
                longestStreak = user?.longestStreak ?: 0,
                totalWordsLearned = totalWords,
                totalSessions = sessions.size,
                totalCorrectAnswers = totalCorrect,
                totalWrongAnswers = totalWrong,
                accuracyPercent = if (totalAnswered > 0) (totalCorrect.toDouble() / totalAnswered * 100).toInt() else 0,
                todayCorrect = 0,
                weeklyAnswers = 0,
                recentSessions = sessions
            )
        } catch (e: Exception) {
            Log.e(tag, "getUserStats ERROR: ${e.message}")
            UserStats()
        }
    }
}
