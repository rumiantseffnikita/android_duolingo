package com.linguaai.app.ui.register

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.linguaai.app.databinding.ActivityRegisterBinding
import com.linguaai.app.ui.languageselection.LanguageSelectionActivity
import com.linguaai.app.ui.login.LoginActivity

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener { register() }
        binding.tvGoToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun register() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        when {
            name.isEmpty() -> showStatus("Введите имя")
            email.isEmpty() -> showStatus("Введите email")
            password.isEmpty() -> showStatus("Введите пароль")
            password != confirmPassword -> showStatus("Пароли не совпадают")
            password.length < 4 -> showStatus("Пароль должен быть не менее 4 символов")
            else -> {
                val intent = Intent(this, LanguageSelectionActivity::class.java).apply {
                    putExtra("name", name)
                    putExtra("email", email)
                    putExtra("password", password)
                }
                startActivity(intent)
            }
        }
    }

    private fun showStatus(message: String) {
        binding.tvStatus.text = message
        binding.tvStatus.visibility = View.VISIBLE
    }
}
