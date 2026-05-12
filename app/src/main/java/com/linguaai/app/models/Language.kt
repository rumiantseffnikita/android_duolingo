package com.linguaai.app.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Language(
    val id: Int = 0,
    val code: String = "",
    val name: String = "",
    @SerialName("native_name")
    val nativeName: String = "",
    @SerialName("flag_emoji")
    val flagEmoji: String? = null,
    @SerialName("is_active")
    val isActive: Boolean? = true,
    @SerialName("created_at")
    val createdAt: String? = null
)
