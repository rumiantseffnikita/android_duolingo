package com.linguaai.app.services

import android.util.Log
import com.linguaai.app.models.*
import io.github.jan.supabase.postgrest.postgrest
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
    ) {
        try {
            var activeSession = client.postgrest["learning_sessions"]
                .select {
                    filter {
                        eq("user_id", userId)
                        isNull("finished_at")
                    }
                }
                .decodeList<LearningSession>()
                .firstOrNull()

            if (activeSession == null) {
                val user = getUserById(userId)
                val langId = user?.targetLanguageId ?: 1
                val sessionId = UUID.randomUUID().toString()
                val sessionMap = mapOf(
                    "id" to sessionId,
                    "user_id" to userId,
                    "language_id" to langId,
                    "started_at" to Instant.now().toString(),
                    "words_studied" to 0,
                    "correct_answers" to 0,
                    "wrong_answers" to 0,
                    "xp_earned" to 0
                )
                client.postgrest["learning_sessions"].insert(sessionMap)
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
            }

            val exerciseMap = mapOf(
                "session_id" to activeSession.id,
                "word_id" to wordId,
                "exercise_type" to "translation",
                "user_answer" to userAnswer,
                "is_correct" to isCorrect,
                "ai_feedback" to aiFeedback,
                "answered_at" to Instant.now().toString(),
                "response_time_ms" to 0
            )
            client.postgrest["exercise_results"].insert(exerciseMap)

            val newCorrect = (activeSession.correctAnswers ?: 0) + if (isCorrect) 1 else 0
            val newWrong = (activeSession.wrongAnswers ?: 0) + if (!isCorrect) 1 else 0
            val newWordsStudied = (activeSession.wordsStudied ?: 0) + 1
            val xpGain = if (isCorrect) 10 else 5
            val newXp = (activeSession.xpEarned ?: 0) + xpGain

            client.postgrest["learning_sessions"].update(
                mapOf(
                    "correct_answers" to newCorrect,
                    "wrong_answers" to newWrong,
                    "words_studied" to newWordsStudied,
                    "xp_earned" to newXp
                )
            ) {
                filter { eq("id", activeSession.id) }
            }

            val currentUser = getUserById(userId)
            if (currentUser != null) {
                client.postgrest["users"].update(
                    mapOf(
                        "total_xp" to ((currentUser.totalXp ?: 0) + xpGain),
                        "last_activity_date" to LocalDate.now().toString(),
                        "updated_at" to Instant.now().toString()
                    )
                ) {
                    filter { eq("id", userId) }
                }
            }

            updateWordProgress(userId, wordId, isCorrect)
        } catch (e: Exception) {
            Log.e(tag, "saveExerciseResult Error: ${e.message}")
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
                    mapOf(
                        "correct_count" to newCorrect,
                        "wrong_count" to newWrong,
                        "repetitions" to newReps,
                        "is_learned" to learned,
                        "last_review" to LocalDate.now().toString(),
                        "next_review" to LocalDate.now().plusDays(if (learned) 7 else 1).toString(),
                        "updated_at" to Instant.now().toString()
                    )
                ) {
                    filter { eq("id", existing.id) }
                }
            } else {
                val progressMap = mapOf(
                    "user_id" to userId,
                    "word_id" to wordId,
                    "repetitions" to 1,
                    "correct_count" to if (isCorrect) 1 else 0,
                    "wrong_count" to if (!isCorrect) 1 else 0,
                    "is_learned" to false,
                    "last_review" to LocalDate.now().toString(),
                    "next_review" to LocalDate.now().plusDays(1).toString(),
                    "created_at" to Instant.now().toString(),
                    "updated_at" to Instant.now().toString()
                )
                client.postgrest["word_progresses"].insert(progressMap)
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
                mapOf(
                    "finished_at" to Instant.now().toString(),
                    "duration_sec" to duration
                )
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
