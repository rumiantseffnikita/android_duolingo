package com.linguaai.app.services

import android.util.Log
import com.linguaai.app.BuildConfig
import com.linguaai.app.models.GeneratedWord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AIService {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiKey: String = BuildConfig.OPENROUTER_API_KEY
    private val useRealAI: Boolean get() = apiKey.isNotBlank()

    private val tag = "AIService"

    private val demoFeedbacks = listOf(
        "Хороший ответ! Продолжайте практиковаться.",
        "Попробуйте обратить внимание на контекст слова.",
        "Отличная работа! Вы делаете успехи.",
        "Не расстраивайтесь, ошибки — часть обучения.",
        "Замечательно! Ваш словарный запас растёт."
    )

    private val demoTips = listOf(
        "Совет: Старайтесь учить слова в контексте предложений",
        "Совет: Повторяйте изученные слова каждый день",
        "Совет: Используйте новые слова в своей речи",
        "Совет: Смотрите фильмы на изучаемом языке",
        "Совет: Читайте книги на изучаемом языке"
    )

    private suspend fun callOpenRouterAPI(prompt: String, retries: Int = 2): String? {
        return withContext(Dispatchers.IO) {
            var lastError: String? = null
            for (attempt in 0..retries) {
                try {
                    if (attempt > 0) {
                        Log.d(tag, "OpenRouter retry attempt $attempt, waiting ${attempt * 10}s...")
                        Thread.sleep(attempt * 10_000L)
                    }

                    val requestBody = JSONObject().apply {
                        put("model", "nvidia/nemotron-3-super-120b-a12b:free")
                        put("messages", JSONArray().apply {
                            put(JSONObject().apply {
                                put("role", "user")
                                put("content", prompt)
                            })
                        })
                        put("temperature", 0.7)
                        put("max_tokens", 1000)
                    }

                    val request = Request.Builder()
                        .url("https://openrouter.ai/api/v1/chat/completions")
                        .addHeader("Authorization", "Bearer $apiKey")
                        .addHeader("Content-Type", "application/json")
                        .addHeader("HTTP-Referer", "https://linguaai.app")
                        .addHeader("X-Title", "LinguaAI")
                        .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                        .build()

                    val response = httpClient.newCall(request).execute()
                    val responseBody = response.body?.string() ?: return@withContext null

                    if (response.code == 429) {
                        Log.w(tag, "OpenRouter rate limited (429), attempt $attempt")
                        lastError = "Rate limit"
                        continue
                    }

                    if (!response.isSuccessful) {
                        Log.e(tag, "OpenRouter error: ${response.code} — $responseBody")
                        return@withContext null
                    }

                    Log.d(tag, "OpenRouter response: ${responseBody.take(300)}")
                    val jsonResponse = JSONObject(responseBody)
                    val content = jsonResponse.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .optString("content", "")
                    if (content.isBlank() || content == "null") {
                        Log.w(tag, "OpenRouter returned empty content, attempt $attempt")
                        lastError = "Empty response"
                        continue
                    }
                    return@withContext content
                } catch (e: Exception) {
                    Log.e(tag, "callOpenRouterAPI error: ${e.message}", e)
                    lastError = e.message
                }
            }
            Log.e(tag, "OpenRouter failed after $retries retries: $lastError")
            null
        }
    }

    suspend fun generateWordsForLesson(
        targetLanguage: String,
        nativeLanguage: String,
        difficultyLevel: String,
        count: Int = 10
    ): List<GeneratedWord> {
        if (useRealAI) {
            val difficultyText = when (difficultyLevel) {
                "beginner" -> "начального уровня"
                "intermediate" -> "среднего уровня"
                "advanced" -> "продвинутого уровня"
                else -> "начального уровня"
            }

            val prompt = """Сгенерируй $count уникальных и полезных слов на языке "$targetLanguage" с переводом на "$nativeLanguage".
Уровень: $difficultyText.
Поле "word" должно содержать слово на $targetLanguage языке, поле "translation" — перевод на $nativeLanguage.

Ответь ТОЛЬКО JSON массивом, без пояснений:
[{"word":"слово","translation":"перевод","transcription":"транскрипция","example_sentence":"пример","example_translation":"перевод примера"}]"""

            val result = callOpenRouterAPI(prompt)
            if (result != null) {
                return parseGeneratedWords(result)
            }
        }

        return getDemoWords()
    }

    suspend fun checkAnswer(word: String, correctTranslation: String, userAnswer: String): String {
        if (useRealAI) {
            val prompt = """Проверь перевод слова.
Слово: $word
Правильный перевод: $correctTranslation
Ответ студента: $userAnswer

Оцени ответ коротко (1-2 предложения). Если ответ правильный или близкий — похвали. Если нет — объясни разницу."""

            val result = callOpenRouterAPI(prompt)
            if (result != null) return result
        }

        return if (userAnswer.trim().equals(correctTranslation.trim(), ignoreCase = true)) {
            "Правильно! Отличная работа!"
        } else {
            "Правильный ответ: $correctTranslation. ${demoFeedbacks.random()}"
        }
    }

    suspend fun generateExampleSentence(word: String, language: String): String {
        if (useRealAI) {
            val prompt = "Придумай простое предложение со словом '$word' на $language языке с переводом на русский. Формат: предложение — перевод"
            val result = callOpenRouterAPI(prompt)
            if (result != null) return result
        }

        return "Пример: \"$word\" — используйте это слово в повседневной речи"
    }

    suspend fun generateWeaknessAnalysis(
        correctAnswers: Int,
        wrongAnswers: Int,
        streakDays: Int
    ): String {
        if (useRealAI) {
            val prompt = """Проанализируй прогресс ученика:
Правильных ответов: $correctAnswers
Ошибок: $wrongAnswers
Дней подряд: $streakDays

Дай краткий анализ (2-3 предложения) и совет для улучшения."""

            val result = callOpenRouterAPI(prompt)
            if (result != null) return result
        }

        val total = correctAnswers + wrongAnswers
        val accuracy = if (total > 0) (correctAnswers.toDouble() / total * 100).toInt() else 0

        return buildString {
            append("Точность: $accuracy%. ")
            if (accuracy >= 80) append("Отличный результат! ") else append("Есть куда расти. ")
            append(demoTips.random())
        }
    }

    suspend fun getDailyTip(): String {
        if (useRealAI) {
            val prompt = "Дай один короткий полезный совет для изучающего иностранный язык (1-2 предложения). Не нумеруй."
            val result = callOpenRouterAPI(prompt)
            if (result != null) return result
        }
        return demoTips.random()
    }

    private fun parseGeneratedWords(json: String): List<GeneratedWord> {
        return try {
            Log.d(tag, "parseGeneratedWords input: ${json.take(500)}")

            // Extract JSON array from response (AI may wrap it in text/markdown)
            var cleaned = json.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            // Find the first '[' and last ']' to extract the JSON array
            val startIdx = cleaned.indexOf('[')
            val endIdx = cleaned.lastIndexOf(']')
            if (startIdx >= 0 && endIdx > startIdx) {
                cleaned = cleaned.substring(startIdx, endIdx + 1)
            }

            val jsonArray = JSONArray(cleaned)
            val words = mutableListOf<GeneratedWord>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                words.add(
                    GeneratedWord(
                        word = obj.optString("word", ""),
                        translation = obj.optString("translation", ""),
                        transcription = obj.optString("transcription"),
                        exampleSentence = obj.optString("example_sentence"),
                        exampleTranslation = obj.optString("example_translation")
                    )
                )
            }
            Log.d(tag, "parseGeneratedWords: parsed ${words.size} words")
            if (words.isNotEmpty()) words else getDemoWords()
        } catch (e: Exception) {
            Log.e(tag, "parseGeneratedWords error: ${e.message}")
            getDemoWords()
        }
    }

    private fun getDemoWords(): List<GeneratedWord> {
        return listOf(
            GeneratedWord("hello", "привет", "The teacher said hello", "Учитель сказал привет", "həˈloʊ"),
            GeneratedWord("world", "мир", "The world is big", "Мир большой", "wɜːrld"),
            GeneratedWord("book", "книга", "I read a book", "Я читаю книгу", "bʊk"),
            GeneratedWord("water", "вода", "I drink water", "Я пью воду", "ˈwɔːtər"),
            GeneratedWord("house", "дом", "This is my house", "Это мой дом", "haʊs"),
            GeneratedWord("cat", "кот", "The cat is sleeping", "Кот спит", "kæt"),
            GeneratedWord("dog", "собака", "The dog is running", "Собака бежит", "dɒɡ"),
            GeneratedWord("sun", "солнце", "The sun is bright", "Солнце яркое", "sʌn"),
            GeneratedWord("tree", "дерево", "A tall tree", "Высокое дерево", "triː"),
            GeneratedWord("food", "еда", "I like food", "Я люблю еду", "fuːd")
        )
    }
}
