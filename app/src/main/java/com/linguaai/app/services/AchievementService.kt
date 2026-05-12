package com.linguaai.app.services

import android.util.Log
import com.linguaai.app.models.Achievement
import com.linguaai.app.models.UserAchievement
import com.linguaai.app.models.WordProgress
import io.github.jan.supabase.postgrest.postgrest
import java.time.Instant

class AchievementService {

    private val client = SupabaseClient.client
    private val tag = "AchievementService"

    data class AchievementWithStatus(
        val achievement: Achievement,
        val isEarned: Boolean,
        val earnedAt: String?
    )

    suspend fun checkAndAwardAchievements(userId: String): List<Achievement> {
        val newlyEarned = mutableListOf<Achievement>()

        try {
            val user = client.postgrest["users"]
                .select { filter { eq("id", userId) } }
                .decodeList<com.linguaai.app.models.User>()
                .firstOrNull() ?: return emptyList()

            val alreadyEarned = client.postgrest["user_achievements"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<UserAchievement>()
                .map { it.achievementId }

            val allAchievements = client.postgrest["achievements"]
                .select()
                .decodeList<Achievement>()

            val wordsLearned = client.postgrest["word_progresses"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<WordProgress>()
                .size

            val sessions = client.postgrest["learning_sessions"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<com.linguaai.app.models.LearningSession>()

            val totalSessions = sessions.size
            val totalCorrect = sessions.sumOf { it.correctAnswers ?: 0 }
            val perfectSessions = sessions.count {
                it.finishedAt != null && (it.wrongAnswers ?: 0) == 0 && (it.wordsStudied ?: 0) > 0
            }

            for (achievement in allAchievements) {
                if (achievement.id in alreadyEarned) continue

                val conditionMet = when (achievement.conditionType) {
                    "words_learned" -> wordsLearned >= (achievement.conditionValue ?: 0)
                    "streak_days" -> (user.streakDays ?: 0) >= (achievement.conditionValue ?: 0)
                    "total_xp" -> (user.totalXp ?: 0) >= (achievement.conditionValue ?: 0)
                    "sessions_count" -> totalSessions >= (achievement.conditionValue ?: 0)
                    "correct_answers" -> totalCorrect >= (achievement.conditionValue ?: 0)
                    "perfect_sessions" -> perfectSessions >= (achievement.conditionValue ?: 0)
                    else -> false
                }

                if (!conditionMet) continue

                client.postgrest["user_achievements"].insert(
                    UserAchievement(
                        userId = userId,
                        achievementId = achievement.id,
                        earnedAt = Instant.now().toString()
                    )
                )

                if ((achievement.xpReward ?: 0) > 0) {
                    client.postgrest["users"].update(
                        mapOf(
                            "total_xp" to ((user.totalXp ?: 0) + (achievement.xpReward ?: 0)),
                            "updated_at" to Instant.now().toString()
                        )
                    ) {
                        filter { eq("id", userId) }
                    }
                }

                newlyEarned.add(achievement)
                Log.d(tag, "Achievement earned: ${achievement.name}")
            }
        } catch (e: Exception) {
            Log.e(tag, "checkAndAwardAchievements ERROR: ${e.message}")
        }

        return newlyEarned
    }

    suspend fun getAllAchievementsWithStatus(userId: String): List<AchievementWithStatus> {
        return try {
            val all = client.postgrest["achievements"]
                .select()
                .decodeList<Achievement>()

            val earned = client.postgrest["user_achievements"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<UserAchievement>()

            val earnedMap = earned.associateBy { it.achievementId }

            all.map { achievement ->
                val ua = earnedMap[achievement.id]
                AchievementWithStatus(
                    achievement = achievement,
                    isEarned = ua != null,
                    earnedAt = ua?.earnedAt
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "getAllAchievementsWithStatus ERROR: ${e.message}")
            emptyList()
        }
    }
}
