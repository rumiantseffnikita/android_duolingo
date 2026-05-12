package com.linguaai.app.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.linguaai.app.R
import com.linguaai.app.databinding.ActivityProfileBinding
import com.linguaai.app.models.Language
import com.linguaai.app.services.AuthService
import com.linguaai.app.ui.login.LoginActivity
import com.linguaai.app.utils.SessionManager
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val authService = AuthService()
    private var languages = listOf<Language>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val user = SessionManager.getUser() ?: run {
            finish()
            return
        }

        binding.btnBack.setOnClickListener { finish() }

        binding.tvName.text = user.name
        binding.tvEmail.text = user.email
        binding.tvXp.text = "⭐ ${user.totalXp ?: 0} XP"
        binding.tvStreak.text = "🔥 ${user.streakDays ?: 0} дней"
        binding.tvMemberSince.text = "Участник с ${user.createdAt?.take(10) ?: "—"}"
        binding.etDailyGoalWords.setText((user.dailyGoalWords ?: 10).toString())
        binding.etDailyGoalMinutes.setText((user.dailyGoalMinutes ?: 15).toString())

        when (user.difficultyLevel) {
            "beginner" -> binding.rbBeginner.isChecked = true
            "intermediate" -> binding.rbIntermediate.isChecked = true
            "advanced" -> binding.rbAdvanced.isChecked = true
            else -> binding.rbBeginner.isChecked = true
        }

        binding.btnSave.setOnClickListener { saveProfile() }
        binding.btnLogout.setOnClickListener { logout() }

        loadLanguages(user.targetLanguageId, user.nativeLanguageId)
    }

    private fun loadLanguages(targetId: Int?, nativeId: Int?) {
        lifecycleScope.launch {
            languages = authService.getAllLanguages()
            val names = languages.map { "${it.flagEmoji ?: ""} ${it.name}" }
            val adapter = ArrayAdapter(this@ProfileActivity, android.R.layout.simple_spinner_dropdown_item, names)
            binding.spinnerTargetLanguage.adapter = adapter
            binding.spinnerNativeLanguage.adapter = adapter

            val targetIdx = languages.indexOfFirst { it.id == targetId }
            val nativeIdx = languages.indexOfFirst { it.id == nativeId }
            if (targetIdx >= 0) binding.spinnerTargetLanguage.setSelection(targetIdx)
            if (nativeIdx >= 0) binding.spinnerNativeLanguage.setSelection(nativeIdx)
        }
    }

    private fun saveProfile() {
        val user = SessionManager.getUser() ?: return

        val difficulty = when (binding.rgDifficulty.checkedRadioButtonId) {
            R.id.rbBeginner -> "beginner"
            R.id.rbIntermediate -> "intermediate"
            R.id.rbAdvanced -> "advanced"
            else -> "beginner"
        }

        val targetIdx = binding.spinnerTargetLanguage.selectedItemPosition
        val nativeIdx = binding.spinnerNativeLanguage.selectedItemPosition
        val targetLangId = if (targetIdx in languages.indices) languages[targetIdx].id else null
        val nativeLangId = if (nativeIdx in languages.indices) languages[nativeIdx].id else null

        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val success = authService.updateUserLanguages(
                user.id, targetLangId, nativeLangId, difficulty
            )

            binding.progressBar.visibility = View.GONE

            if (success) {
                val updatedUser = user.copy(
                    targetLanguageId = targetLangId ?: user.targetLanguageId,
                    nativeLanguageId = nativeLangId ?: user.nativeLanguageId,
                    difficultyLevel = difficulty,
                    dailyGoalWords = binding.etDailyGoalWords.text.toString().toIntOrNull() ?: 10,
                    dailyGoalMinutes = binding.etDailyGoalMinutes.text.toString().toIntOrNull() ?: 15
                )
                SessionManager.saveUser(updatedUser)

                binding.tvSaveMessage.text = "✅ Сохранено!"
                binding.tvSaveMessage.setTextColor(getColor(R.color.green))
                binding.tvSaveMessage.visibility = View.VISIBLE
            } else {
                binding.tvSaveMessage.text = "Ошибка сохранения"
                binding.tvSaveMessage.setTextColor(getColor(R.color.red))
                binding.tvSaveMessage.visibility = View.VISIBLE
            }
        }
    }

    private fun logout() {
        SessionManager.logout()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
