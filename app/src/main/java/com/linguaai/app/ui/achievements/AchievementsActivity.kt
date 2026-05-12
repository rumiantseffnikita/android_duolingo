package com.linguaai.app.ui.achievements

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.linguaai.app.databinding.ActivityAchievementsBinding
import com.linguaai.app.services.AchievementService
import com.linguaai.app.utils.SessionManager
import kotlinx.coroutines.launch

class AchievementsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAchievementsBinding
    private val achievementService = AchievementService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAchievementsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.rvAchievements.layoutManager = LinearLayoutManager(this)

        loadAchievements()
    }

    private fun loadAchievements() {
        val user = SessionManager.getUser() ?: return
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val data = achievementService.getAllAchievementsWithStatus(user.id)

            val earnedCount = data.count { it.isEarned }
            binding.tvCounter.text = "$earnedCount / ${data.size}"

            binding.rvAchievements.adapter = AchievementsAdapter(data)
            binding.progressBar.visibility = View.GONE
        }
    }
}
