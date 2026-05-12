package com.linguaai.app.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Achievement(
    val id: Int = 0,
    val code: String = "",
    val name: String = "",
    val description: String? = null,
    @SerialName("icon_emoji")
    val iconEmoji: String? = null,
    @SerialName("xp_reward")
    val xpReward: Int? = 0,
    @SerialName("condition_type")
    val conditionType: String? = null,
    @SerialName("condition_value")
    val conditionValue: Int? = null
)
