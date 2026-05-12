package com.linguaai.app.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WordProgress(
    val id: Int = 0,
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("word_id")
    val wordId: Int = 0,
    val repetitions: Int? = 0,
    @SerialName("easiness_factor")
    val easinessFactor: Double? = null,
    @SerialName("interval_days")
    val intervalDays: Int? = null,
    @SerialName("next_review")
    val nextReview: String? = null,
    @SerialName("last_review")
    val lastReview: String? = null,
    @SerialName("correct_count")
    val correctCount: Int? = 0,
    @SerialName("wrong_count")
    val wrongCount: Int? = 0,
    @SerialName("is_learned")
    val isLearned: Boolean? = false,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)
