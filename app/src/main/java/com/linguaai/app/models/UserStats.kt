package com.linguaai.app.models

data class UserStats(
    val totalXp: Int = 0,
    val streakDays: Int = 0,
    val longestStreak: Int = 0,
    val totalWordsLearned: Int = 0,
    val totalSessions: Int = 0,
    val totalCorrectAnswers: Int = 0,
    val totalWrongAnswers: Int = 0,
    val accuracyPercent: Int = 0,
    val todayCorrect: Int = 0,
    val weeklyAnswers: Int = 0,
    val recentSessions: List<LearningSession> = emptyList()
)
