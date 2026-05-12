package com.linguaai.app.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.linguaai.app.R
import com.linguaai.app.databinding.ActivityDashboardBinding
import com.linguaai.app.services.AIService
import com.linguaai.app.services.WordService
import com.linguaai.app.ui.achievements.AchievementsActivity
import com.linguaai.app.ui.lesson.LessonActivity
import com.linguaai.app.ui.profile.ProfileActivity
import com.linguaai.app.ui.statistics.StatisticsActivity
import com.linguaai.app.ui.vocabulary.VocabularyActivity
import com.linguaai.app.utils.SessionManager
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val wordService = WordService()
    private val aiService = AIService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNav()
        setupButtons()
        loadDashboardData()
    }

    override fun onResume() {
        super.onResume()
        loadDashboardData()
    }

    private fun setupButtons() {
        binding.btnStartLesson.setOnClickListener {
            startActivity(Intent(this, LessonActivity::class.java))
        }
        binding.btnRefresh.setOnClickListener { loadDashboardData() }
    }

    private fun setupBottomNav() {
        binding.bottomNav.selectedItemId = R.id.nav_home
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_stats -> {
                    startActivity(Intent(this, StatisticsActivity::class.java))
                    true
                }
                R.id.nav_achievements -> {
                    startActivity(Intent(this, AchievementsActivity::class.java))
                    true
                }
                R.id.nav_vocabulary -> {
                    startActivity(Intent(this, VocabularyActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun loadDashboardData() {
        val user = SessionManager.getUser() ?: return
        binding.progressBar.visibility = View.VISIBLE

        binding.tvUserName.text = user.name

        lifecycleScope.launch {
            val freshUser = wordService.getUserById(user.id)
            val displayUser = freshUser ?: user

            val totalXp = displayUser.totalXp ?: 0
            val streakDays = displayUser.streakDays ?: 0
            val wordsLearned = wordService.getLearnedWordsCount(user.id)
            val todayProgress = wordService.getTodayProgress(user.id)
            val dailyGoal = displayUser.dailyGoalWords ?: 10
            val tip = aiService.getDailyTip()

            binding.tvStreakDays.text = streakDays.toString()
            binding.tvTotalXp.text = totalXp.toString()
            binding.tvWordsLearned.text = wordsLearned.toString()
            binding.tvStreakBanner.text = getString(R.string.streak_banner, streakDays)

            val progressPercent = if (dailyGoal > 0) (todayProgress * 100 / dailyGoal).coerceAtMost(100) else 0
            binding.progressDailyGoal.progress = progressPercent
            binding.tvDailyProgress.text = "$todayProgress / $dailyGoal слов сегодня"

            binding.tvDailyTip.text = tip
            binding.progressBar.visibility = View.GONE

            if (freshUser != null) {
                SessionManager.saveUser(freshUser)
            }
        }
    }
}
