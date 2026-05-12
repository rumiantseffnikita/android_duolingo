package com.linguaai.app.services

import android.util.Log
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

    private var apiKey: String = ""
    private var baseUrl: String = "https://foundation-models.api.cloud.ru/v1"
    private var model: String = "GigaChat-Lightning"
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

    private suspend fun callCloudRuAPI(prompt: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val requestBody = JSONObject().apply {
                    put("model", model)
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", prompt)
                        })
                    })
                    put("temperature", 0.7)
                    put("max_tokens", 1000)
                    put("stream", false)
                }

                val request = Request.Builder()
                    .url("$baseUrl/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Accept", "application/json")
                    .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: return@withContext null

                if (!response.isSuccessful) {
                    Log.e(tag, "Cloud.ru API error: ${response.code}")
                    return@withContext null
                }

                val jsonResponse = JSONObject(responseBody)
                jsonResponse.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
            } catch (e: Exception) {
                Log.e(tag, "callCloudRuAPI error: ${e.message}")
                null
            }
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

            val prompt = """Сгенерируй $count уникальных и полезных слов для изучения $targetLanguage языка.
Родной язык студента: $nativeLanguage. Уровень: $difficultyText.

Верни СТРОГО в формате JSON массив:
[{"word":"слово","translation":"перевод","transcription":"транскрипция","example_sentence":"пример","example_translation":"перевод примера"}]"""

            val result = callCloudRuAPI(prompt)
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

            val result = callCloudRuAPI(prompt)
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
            val result = callCloudRuAPI(prompt)
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

            val result = callCloudRuAPI(prompt)
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
        return demoTips.random()
    }

    private fun parseGeneratedWords(json: String): List<GeneratedWord> {
        return try {
            val cleaned = json.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

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
            words
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
