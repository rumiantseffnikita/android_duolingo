package com.linguaai.app.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExerciseResult(
    val id: Int = 0,
    @SerialName("session_id")
    val sessionId: String = "",
    @SerialName("word_id")
    val wordId: Int = 0,
    @SerialName("exercise_type")
    val exerciseType: String = "translation",
    @SerialName("user_answer")
    val userAnswer: String? = null,
    @SerialName("is_correct")
    val isCorrect: Boolean = false,
    @SerialName("response_time_ms")
    val responseTimeMs: Int? = null,
    @SerialName("ai_feedback")
    val aiFeedback: String? = null,
    @SerialName("answered_at")
    val answeredAt: String? = null
)
