package com.linguaai.app.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.linguaai.app.databinding.ActivityLoginBinding
import com.linguaai.app.services.AuthService
import com.linguaai.app.ui.dashboard.DashboardActivity
import com.linguaai.app.ui.register.RegisterActivity
import com.linguaai.app.utils.SessionManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val authService = AuthService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SessionManager.init(this)

        if (SessionManager.isLoggedIn()) {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener { login() }
        binding.tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun login() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty()) {
            showStatus("Введите email")
            return
        }
        if (password.isEmpty()) {
            showStatus("Введите пароль")
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            val result = authService.login(email, password)

            setLoading(false)

            if (result.success && result.user != null) {
                SessionManager.saveUser(result.user)
                startActivity(Intent(this@LoginActivity, DashboardActivity::class.java))
                finish()
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
        binding.btnLogin.isEnabled = !loading
        if (loading) binding.tvStatus.visibility = View.GONE
    }
}
