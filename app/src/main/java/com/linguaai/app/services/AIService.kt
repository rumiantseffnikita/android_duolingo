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
                        put("max_tokens", 4000)
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

            val result = callOpenRouterAPI(prompt, retries = 4)
            if (result != null) {
                val parsed = parseGeneratedWords(result)
                if (parsed.isNotEmpty() && parsed[0].word != "hello") {
                    return parsed
                }
            }
        }

        return getDemoWords(targetLanguage)
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
            } else if (startIdx >= 0) {
                // JSON was truncated — try to salvage complete objects
                cleaned = cleaned.substring(startIdx)
                val lastComplete = cleaned.lastIndexOf('}')
                if (lastComplete > 0) {
                    cleaned = cleaned.substring(0, lastComplete + 1) + "]"
                }
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

    private fun getDemoWords(targetLanguage: String = "Английский"): List<GeneratedWord> {
        return when {
            targetLanguage.contains("Испан", true) -> listOf(
                GeneratedWord("hola", "привет", "¡Hola, amigo!", "Привет, друг!", "[ˈola]"),
                GeneratedWord("gracias", "спасибо", "Gracias por todo.", "Спасибо за всё.", "[ˈɡɾasjas]"),
                GeneratedWord("agua", "вода", "Quiero agua.", "Я хочу воду.", "[ˈaɣwa]"),
                GeneratedWord("casa", "дом", "Mi casa es grande.", "Мой дом большой.", "[ˈkasa]"),
                GeneratedWord("gato", "кот", "El gato duerme.", "Кот спит.", "[ˈɡato]"),
                GeneratedWord("perro", "собака", "El perro corre.", "Собака бежит.", "[ˈpero]"),
                GeneratedWord("sol", "солнце", "El sol brilla.", "Солнце светит.", "[sol]"),
                GeneratedWord("libro", "книга", "Leo un libro.", "Я читаю книгу.", "[ˈliβɾo]"),
                GeneratedWord("amigo", "друг", "Es mi amigo.", "Это мой друг.", "[aˈmiɣo]"),
                GeneratedWord("comida", "еда", "La comida está rica.", "Еда вкусная.", "[koˈmiða]")
            )
            targetLanguage.contains("Итал", true) -> listOf(
                GeneratedWord("ciao", "привет", "Ciao, come stai?", "Привет, как дела?", "[tʃao]"),
                GeneratedWord("grazie", "спасибо", "Grazie mille!", "Большое спасибо!", "[ˈɡrattsje]"),
                GeneratedWord("acqua", "вода", "Vorrei dell'acqua.", "Я хочу воду.", "[ˈakkwa]"),
                GeneratedWord("casa", "дом", "La mia casa è bella.", "Мой дом красивый.", "[ˈkaːza]"),
                GeneratedWord("gatto", "кот", "Il gatto dorme.", "Кот спит.", "[ˈɡatto]"),
                GeneratedWord("cane", "собака", "Il cane corre.", "Собака бежит.", "[ˈkaːne]"),
                GeneratedWord("sole", "солнце", "Il sole splende.", "Солнце светит.", "[ˈsoːle]"),
                GeneratedWord("libro", "книга", "Leggo un libro.", "Я читаю книгу.", "[ˈliːbro]"),
                GeneratedWord("amico", "друг", "È il mio amico.", "Это мой друг.", "[aˈmiːko]"),
                GeneratedWord("cibo", "еда", "Il cibo è buono.", "Еда вкусная.", "[ˈtʃiːbo]")
            )
            targetLanguage.contains("Франц", true) -> listOf(
                GeneratedWord("bonjour", "привет", "Bonjour, comment ça va?", "Привет, как дела?", "[bɔ̃ʒuʁ]"),
                GeneratedWord("merci", "спасибо", "Merci beaucoup!", "Большое спасибо!", "[mɛʁsi]"),
                GeneratedWord("eau", "вода", "Je veux de l'eau.", "Я хочу воду.", "[o]"),
                GeneratedWord("maison", "дом", "Ma maison est grande.", "Мой дом большой.", "[mɛzɔ̃]"),
                GeneratedWord("chat", "кот", "Le chat dort.", "Кот спит.", "[ʃa]"),
                GeneratedWord("chien", "собака", "Le chien court.", "Собака бежит.", "[ʃjɛ̃]"),
                GeneratedWord("soleil", "солнце", "Le soleil brille.", "Солнце светит.", "[sɔlɛj]"),
                GeneratedWord("livre", "книга", "Je lis un livre.", "Я читаю книгу.", "[livʁ]"),
                GeneratedWord("ami", "друг", "C'est mon ami.", "Это мой друг.", "[ami]"),
                GeneratedWord("nourriture", "еда", "La nourriture est bonne.", "Еда вкусная.", "[nuʁityʁ]")
            )
            targetLanguage.contains("Немец", true) -> listOf(
                GeneratedWord("hallo", "привет", "Hallo, wie geht's?", "Привет, как дела?", "[ˈhalo]"),
                GeneratedWord("danke", "спасибо", "Danke schön!", "Большое спасибо!", "[ˈdaŋkə]"),
                GeneratedWord("Wasser", "вода", "Ich möchte Wasser.", "Я хочу воду.", "[ˈvasɐ]"),
                GeneratedWord("Haus", "дом", "Mein Haus ist groß.", "Мой дом большой.", "[haʊs]"),
                GeneratedWord("Katze", "кот", "Die Katze schläft.", "Кот спит.", "[ˈkatsə]"),
                GeneratedWord("Hund", "собака", "Der Hund läuft.", "Собака бежит.", "[hʊnt]"),
                GeneratedWord("Sonne", "солнце", "Die Sonne scheint.", "Солнце светит.", "[ˈzɔnə]"),
                GeneratedWord("Buch", "книга", "Ich lese ein Buch.", "Я читаю книгу.", "[buːx]"),
                GeneratedWord("Freund", "друг", "Er ist mein Freund.", "Он мой друг.", "[fʁɔʏnt]"),
                GeneratedWord("Essen", "еда", "Das Essen ist gut.", "Еда вкусная.", "[ˈɛsn̩]")
            )
            targetLanguage.contains("Китай", true) -> listOf(
                GeneratedWord("你好", "привет", "你好，朋友！", "Привет, друг!", "[nǐ hǎo]"),
                GeneratedWord("谢谢", "спасибо", "谢谢你！", "Спасибо тебе!", "[xiè xie]"),
                GeneratedWord("水", "вода", "我要水。", "Я хочу воду.", "[shuǐ]"),
                GeneratedWord("家", "дом", "这是我的家。", "Это мой дом.", "[jiā]"),
                GeneratedWord("猫", "кот", "猫在睡觉。", "Кот спит.", "[māo]"),
                GeneratedWord("狗", "собака", "狗在跑。", "Собака бежит.", "[gǒu]"),
                GeneratedWord("太阳", "солнце", "太阳很亮。", "Солнце яркое.", "[tài yáng]"),
                GeneratedWord("书", "книга", "我在读书。", "Я читаю книгу.", "[shū]"),
                GeneratedWord("朋友", "друг", "他是我的朋友。", "Он мой друг.", "[péng you]"),
                GeneratedWord("食物", "еда", "食物很好吃。", "Еда вкусная.", "[shí wù]")
            )
            targetLanguage.contains("Япон", true) -> listOf(
                GeneratedWord("こんにちは", "привет", "こんにちは、元気ですか？", "Привет, как дела?", "[konnichiwa]"),
                GeneratedWord("ありがとう", "спасибо", "ありがとうございます！", "Большое спасибо!", "[arigatō]"),
                GeneratedWord("水", "вода", "水をください。", "Воду, пожалуйста.", "[mizu]"),
                GeneratedWord("家", "дом", "私の家は大きいです。", "Мой дом большой.", "[ie]"),
                GeneratedWord("猫", "кот", "猫が寝ています。", "Кот спит.", "[neko]"),
                GeneratedWord("犬", "собака", "犬が走っています。", "Собака бежит.", "[inu]"),
                GeneratedWord("太陽", "солнце", "太陽が輝いています。", "Солнце светит.", "[taiyō]"),
                GeneratedWord("本", "книга", "本を読んでいます。", "Я читаю книгу.", "[hon]"),
                GeneratedWord("友達", "друг", "彼は私の友達です。", "Он мой друг.", "[tomodachi]"),
                GeneratedWord("食べ物", "еда", "食べ物はおいしいです。", "Еда вкусная.", "[tabemono]")
            )
            targetLanguage.contains("Корей", true) -> listOf(
                GeneratedWord("안녕하세요", "привет", "안녕하세요, 잘 지내세요?", "Привет, как дела?", "[annyeonghaseyo]"),
                GeneratedWord("감사합니다", "спасибо", "감사합니다!", "Спасибо!", "[gamsahamnida]"),
                GeneratedWord("물", "вода", "물 주세요.", "Воду, пожалуйста.", "[mul]"),
                GeneratedWord("집", "дом", "우리 집은 커요.", "Наш дом большой.", "[jip]"),
                GeneratedWord("고양이", "кот", "고양이가 자고 있어요.", "Кот спит.", "[goyangi]"),
                GeneratedWord("개", "собака", "개가 달려요.", "Собака бежит.", "[gae]"),
                GeneratedWord("해", "солнце", "해가 밝아요.", "Солнце яркое.", "[hae]"),
                GeneratedWord("책", "книга", "책을 읽어요.", "Я читаю книгу.", "[chaek]"),
                GeneratedWord("친구", "друг", "그는 내 친구예요.", "Он мой друг.", "[chingu]"),
                GeneratedWord("음식", "еда", "음식이 맛있어요.", "Еда вкусная.", "[eumsik]")
            )
            else -> listOf(
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
}
