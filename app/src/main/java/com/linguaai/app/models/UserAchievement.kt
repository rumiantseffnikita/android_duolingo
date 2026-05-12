package com.linguaai.app.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserAchievement(
    val id: Int = 0,
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("achievement_id")
    val achievementId: Int = 0,
    @SerialName("earned_at")
    val earnedAt: String? = null
)
