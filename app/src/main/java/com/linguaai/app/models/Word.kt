package com.linguaai.app.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Word(
    val id: Int = 0,
    @SerialName("language_id")
    val languageId: Int = 0,
    @SerialName("topic_id")
    val topicId: Int? = null,
    val word: String = "",
    val translation: String = "",
    val transcription: String? = null,
    @SerialName("example_sentence")
    val exampleSentence: String? = null,
    @SerialName("example_translation")
    val exampleTranslation: String? = null,
    @SerialName("frequency_rank")
    val frequencyRank: Int? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)
