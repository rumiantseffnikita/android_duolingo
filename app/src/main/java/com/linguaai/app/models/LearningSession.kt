package com.linguaai.app.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LearningSession(
    val id: String = "",
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("language_id")
    val languageId: Int = 0,
    @SerialName("started_at")
    val startedAt: String? = null,
    @SerialName("finished_at")
    val finishedAt: String? = null,
    @SerialName("duration_sec")
    val durationSec: Int? = null,
    @SerialName("words_studied")
    val wordsStudied: Int? = 0,
    @SerialName("correct_answers")
    val correctAnswers: Int? = 0,
    @SerialName("wrong_answers")
    val wrongAnswers: Int? = 0,
    @SerialName("xp_earned")
    val xpEarned: Int? = 0
)
