package com.linguaai.app.ui.statistics

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.linguaai.app.databinding.ActivityStatisticsBinding
import com.linguaai.app.services.AIService
import com.linguaai.app.services.StatisticsService
import com.linguaai.app.utils.SessionManager
import kotlinx.coroutines.launch

class StatisticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatisticsBinding
    private val statsService = StatisticsService()
    private val aiService = AIService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        loadStats()
    }

    private fun loadStats() {
        val user = SessionManager.getUser() ?: return
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val stats = statsService.getUserStats(user.id)

            binding.tvTotalXp.text = stats.totalXp.toString()
            binding.tvStreakDays.text = stats.streakDays.toString()
            binding.tvWordsLearned.text = stats.totalWordsLearned.toString()
            binding.tvAccuracy.text = "${stats.accuracyPercent}%"

            val analysis = aiService.generateWeaknessAnalysis(
                stats.totalCorrectAnswers,
                stats.totalWrongAnswers,
                stats.streakDays
            )
            binding.tvAiAnalysis.text = analysis

            binding.progressBar.visibility = View.GONE
        }
    }
}
