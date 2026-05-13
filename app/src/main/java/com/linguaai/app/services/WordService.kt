package com.linguaai.app.services

import android.util.Log
import com.linguaai.app.models.*
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class WordService {

    private val client = SupabaseClient.client
    private val tag = "WordService"

    suspend fun getUserById(userId: String): User? {
        return try {
            client.postgrest["users"]
                .select { filter { eq("id", userId) } }
                .decodeList<User>()
                .firstOrNull()
        } catch (e: Exception) {
            Log.e(tag, "getUserById Error: ${e.message}")
            null
        }
    }

    suspend fun getWordsForLesson(userId: String): List<Word> {
        return try {
            client.postgrest["words"]
                .select { limit(10) }
                .decodeList()
        } catch (e: Exception) {
            Log.e(tag, "getWordsForLesson Error: ${e.message}")
            emptyList()
        }
    }

    suspend fun getAnyWords(count: Int): List<Word> {
        return try {
            client.postgrest["words"]
                .select { limit(count.toLong()) }
                .decodeList()
        } catch (e: Exception) {
            Log.e(tag, "getAnyWords Error: ${e.message}")
            emptyList()
        }
    }

    suspend fun saveExerciseResult(
        userId: String,
        wordId: Int,
        isCorrect: Boolean,
        userAnswer: String,
        aiFeedback: String
    ): String? {
        try {
            Log.d(tag, "saveExerciseResult: userId=$userId, wordId=$wordId, isCorrect=$isCorrect")

            var activeSession = try {
                client.postgrest["learning_sessions"]
                    .select {
                        filter {
                            eq("user_id", userId)
                            isNull("finished_at")
                        }
                    }
                    .decodeList<LearningSession>()
                    .firstOrNull()
            } catch (e: Exception) {
                Log.e(tag, "Error fetching active session: ${e.message}", e)
                null
            }

            if (activeSession == null) {
                val user = getUserById(userId)
                val langId = user?.targetLanguageId ?: 1
                val sessionId = UUID.randomUUID().toString()
                val sessionJson = buildJsonObject {
                    put("id", sessionId)
                    put("user_id", userId)
                    put("language_id", langId)
                    put("started_at", Instant.now().toString())
                    put("words_studied", 0)
                    put("correct_answers", 0)
                    put("wrong_answers", 0)
                    put("xp_earned", 0)
                }
                Log.d(tag, "Creating new session: $sessionJson")
                client.postgrest["learning_sessions"].insert(sessionJson)
                activeSession = LearningSession(
                    id = sessionId,
                    userId = userId,
                    languageId = langId,
                    startedAt = Instant.now().toString(),
                    wordsStudied = 0,
                    correctAnswers = 0,
                    wrongAnswers = 0,
                    xpEarned = 0
                )
                Log.d(tag, "Session created: $sessionId")
            }

            if (wordId > 0) {
                val exerciseJson = buildJsonObject {
                    put("session_id", activeSession.id)
                    put("word_id", wordId)
                    put("exercise_type", "translation")
                    put("user_answer", userAnswer)
                    put("is_correct", isCorrect)
                    put("ai_feedback", aiFeedback)
                    put("answered_at", Instant.now().toString())
                    put("response_time_ms", 0)
                }
                Log.d(tag, "Inserting exercise result for session: ${activeSession.id}")
                client.postgrest["exercise_results"].insert(exerciseJson)
                Log.d(tag, "Exercise result inserted")
            } else {
                Log.d(tag, "Skipping exercise_results insert for AI-generated word (id=$wordId)")
            }

            val newCorrect = (activeSession.correctAnswers ?: 0) + if (isCorrect) 1 else 0
            val newWrong = (activeSession.wrongAnswers ?: 0) + if (!isCorrect) 1 else 0
            val newWordsStudied = (activeSession.wordsStudied ?: 0) + 1
            val xpGain = if (isCorrect) 10 else 5
            val newXp = (activeSession.xpEarned ?: 0) + xpGain

            client.postgrest["learning_sessions"].update(
                buildJsonObject {
                    put("correct_answers", newCorrect)
                    put("wrong_answers", newWrong)
                    put("words_studied", newWordsStudied)
                    put("xp_earned", newXp)
                }
            ) {
                filter { eq("id", activeSession.id) }
            }
            Log.d(tag, "Session updated")

            val currentUser = getUserById(userId)
            if (currentUser != null) {
                client.postgrest["users"].update(
                    buildJsonObject {
                        put("total_xp", (currentUser.totalXp ?: 0) + xpGain)
                        put("last_activity_date", LocalDate.now().toString())
                        put("updated_at", Instant.now().toString())
                    }
                ) {
                    filter { eq("id", userId) }
                }
                Log.d(tag, "User XP updated")
            }

            if (wordId > 0) {
                updateWordProgress(userId, wordId, isCorrect)
            }
            Log.d(tag, "saveExerciseResult completed successfully")
            return null
        } catch (e: Exception) {
            Log.e(tag, "saveExerciseResult Error: ${e.message}", e)
            return e.message
        }
    }

    private suspend fun updateWordProgress(userId: String, wordId: Int, isCorrect: Boolean) {
        try {
            val existing = client.postgrest["word_progresses"]
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("word_id", wordId)
                    }
                }
                .decodeList<WordProgress>()
                .firstOrNull()

            if (existing != null) {
                val newCorrect = (existing.correctCount ?: 0) + if (isCorrect) 1 else 0
                val newWrong = (existing.wrongCount ?: 0) + if (!isCorrect) 1 else 0
                val newReps = (existing.repetitions ?: 0) + 1
                val learned = newCorrect >= 3

                client.postgrest["word_progresses"].update(
                    buildJsonObject {
                        put("correct_count", newCorrect)
                        put("wrong_count", newWrong)
                        put("repetitions", newReps)
                        put("is_learned", learned)
                        put("last_review", LocalDate.now().toString())
                        put("next_review", LocalDate.now().plusDays(if (learned) 7 else 1).toString())
                        put("updated_at", Instant.now().toString())
                    }
                ) {
                    filter { eq("id", existing.id) }
                }
            } else {
                val progressJson = buildJsonObject {
                    put("user_id", userId)
                    put("word_id", wordId)
                    put("repetitions", 1)
                    put("correct_count", if (isCorrect) 1 else 0)
                    put("wrong_count", if (!isCorrect) 1 else 0)
                    put("is_learned", false)
                    put("last_review", LocalDate.now().toString())
                    put("next_review", LocalDate.now().plusDays(1).toString())
                    put("created_at", Instant.now().toString())
                    put("updated_at", Instant.now().toString())
                }
                client.postgrest["word_progresses"].insert(progressJson)
            }
        } catch (e: Exception) {
            Log.e(tag, "updateWordProgress Error: ${e.message}")
        }
    }

    suspend fun getLearnedWordsCount(userId: String): Int {
        return try {
            client.postgrest["word_progresses"]
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<WordProgress>()
                .size
        } catch (e: Exception) {
            Log.e(tag, "getLearnedWordsCount Error: ${e.message}")
            0
        }
    }

    suspend fun getTodayProgress(userId: String): Int {
        return try {
            val today = LocalDate.now().toString()
            client.postgrest["word_progresses"]
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("last_review", today)
                    }
                }
                .decodeList<WordProgress>()
                .size
        } catch (e: Exception) {
            0
        }
    }

    suspend fun getWordsToReview(userId: String, count: Int): List<Word> {
        return try {
            val today = LocalDate.now().toString()
            val progresses = client.postgrest["word_progresses"]
                .select {
                    filter {
                        eq("user_id", userId)
                        lte("next_review", today)
                    }
                    limit(count.toLong())
                }
                .decodeList<WordProgress>()

            if (progresses.isEmpty()) return emptyList()

            val wordIds = progresses.map { it.wordId }
            client.postgrest["words"]
                .select {
                    filter { isIn("id", wordIds) }
                }
                .decodeList()
        } catch (e: Exception) {
            Log.e(tag, "getWordsToReview Error: ${e.message}")
            emptyList()
        }
    }

    suspend fun finishSession(userId: String) {
        try {
            val session = client.postgrest["learning_sessions"]
                .select {
                    filter {
                        eq("user_id", userId)
                        isNull("finished_at")
                    }
                }
                .decodeList<LearningSession>()
                .firstOrNull() ?: return

            val startedAt = Instant.parse(session.startedAt ?: return)
            val duration = (Instant.now().epochSecond - startedAt.epochSecond).toInt()

            client.postgrest["learning_sessions"].update(
                buildJsonObject {
                    put("finished_at", Instant.now().toString())
                    put("duration_sec", duration)
                }
            ) {
                filter { eq("id", session.id) }
            }
        } catch (e: Exception) {
            Log.e(tag, "finishSession Error: ${e.message}")
        }
    }

    data class VocabularyItem(val word: Word, val progress: WordProgress?)

    suspend fun getVocabulary(userId: String): List<Pair<Topic?, List<VocabularyItem>>> {
        return try {
            val words = client.postgrest["words"]
                .select()
                .decodeList<Word>()

            val progresses = client.postgrest["word_progresses"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<WordProgress>()

            val topics = client.postgrest["topics"]
                .select()
                .decodeList<Topic>()

            val progressMap = progresses.associateBy { it.wordId }
            val topicMap = topics.associateBy { it.id }

            words.groupBy { it.topicId }
                .map { (topicId, wordList) ->
                    val topic = topicId?.let { topicMap[it] }
                    val items = wordList.map { w ->
                        VocabularyItem(w, progressMap[w.id])
                    }
                    topic to items
                }
        } catch (e: Exception) {
            Log.e(tag, "getVocabulary Error: ${e.message}")
            emptyList()
        }
    }
}
