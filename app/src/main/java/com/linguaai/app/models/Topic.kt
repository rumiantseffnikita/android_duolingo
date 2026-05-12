package com.linguaai.app.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Topic(
    val id: Int = 0,
    @SerialName("language_id")
    val languageId: Int = 0,
    val name: String = "",
    val description: String? = null,
    @SerialName("icon_emoji")
    val iconEmoji: String? = null,
    @SerialName("sort_order")
    val sortOrder: Int? = 0
)
