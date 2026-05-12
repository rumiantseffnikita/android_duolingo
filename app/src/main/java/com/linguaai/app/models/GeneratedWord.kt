package com.linguaai.app.models

data class GeneratedWord(
    val word: String = "",
    val translation: String = "",
    val exampleSentence: String? = null,
    val exampleTranslation: String? = null,
    val transcription: String? = null
)
