package com.linguaai.app.ui.languageselection

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.linguaai.app.R
import com.linguaai.app.databinding.ActivityLanguageSelectionBinding
import com.linguaai.app.models.Language
import com.linguaai.app.services.AuthService
import com.linguaai.app.ui.dashboard.DashboardActivity
import com.linguaai.app.utils.SessionManager
import kotlinx.coroutines.launch

class LanguageSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLanguageSelectionBinding
    private val authService = AuthService()
    private var languages = listOf<Language>()

    private val name by lazy { intent.getStringExtra("name") ?: "" }
    private val email by lazy { intent.getStringExtra("email") ?: "" }
    private val password by lazy { intent.getStringExtra("password") ?: "" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnComplete.setOnClickListener { completeRegistration() }
        binding.btnBack.setOnClickListener { finish() }

        loadLanguages()
    }

    private fun loadLanguages() {
        setLoading(true)
        lifecycleScope.launch {
            languages = authService.getAllLanguages()
            setLoading(false)

            val names = languages.map { "${it.flagEmoji ?: ""} ${it.name}" }
            val adapter = ArrayAdapter(this@LanguageSelectionActivity, android.R.layout.simple_spinner_dropdown_item, names)
            binding.spinnerTargetLanguage.adapter = adapter
            binding.spinnerNativeLanguage.adapter = adapter

            val enIndex = languages.indexOfFirst { it.code == "en" }
            val ruIndex = languages.indexOfFirst { it.code == "ru" }
            if (enIndex >= 0) binding.spinnerTargetLanguage.setSelection(enIndex)
            if (ruIndex >= 0) binding.spinnerNativeLanguage.setSelection(ruIndex)
        }
    }

    private fun completeRegistration() {
        val targetIdx = binding.spinnerTargetLanguage.selectedItemPosition
        val nativeIdx = binding.spinnerNativeLanguage.selectedItemPosition

        if (targetIdx < 0 || targetIdx >= languages.size) {
            showStatus("Выберите язык для изучения")
            return
        }
        if (nativeIdx < 0 || nativeIdx >= languages.size) {
            showStatus("Выберите родной язык")
            return
        }

        val difficulty = when (binding.rgDifficulty.checkedRadioButtonId) {
            R.id.rbBeginner -> "beginner"
            R.id.rbIntermediate -> "intermediate"
            R.id.rbAdvanced -> "advanced"
            else -> "beginner"
        }

        setLoading(true)

        lifecycleScope.launch {
            val result = authService.registerWithLanguage(
                name, email, password,
                languages[targetIdx].id,
                languages[nativeIdx].id,
                difficulty
            )

            setLoading(false)

            if (result.success && result.user != null) {
                SessionManager.saveUser(result.user)
                val intent = Intent(this@LanguageSelectionActivity, DashboardActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            } else {
                showStatus(result.message)
            }
        }
    }

    private fun showStatus(message: String) {
        binding.tvStatus.text = message
        binding.tvStatus.visibility = View.VISIBLE
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnComplete.isEnabled = !loading
    }
}
