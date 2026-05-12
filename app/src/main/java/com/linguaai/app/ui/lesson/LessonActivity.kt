package com.linguaai.app.ui.lesson

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.linguaai.app.databinding.ActivityLessonBinding
import com.linguaai.app.models.Word
import com.linguaai.app.services.AIService
import com.linguaai.app.services.AchievementService
import com.linguaai.app.services.WordService
import com.linguaai.app.utils.SessionManager
import kotlinx.coroutines.launch

class LessonActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLessonBinding
    private val wordService = WordService()
    private val aiService = AIService()
    private val achievementService = AchievementService()

    private var words = listOf<Word>()
    private var currentIndex = 0
    private var score = 0
    private var total = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLessonBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCheck.setOnClickListener { checkAnswer() }
        binding.btnNext.setOnClickListener { nextWord() }
        binding.btnEndLesson.setOnClickListener { endLesson() }
        binding.btnShowExample.setOnClickListener { loadExample() }
        binding.btnAbort.setOnClickListener { abortLesson() }

        loadWords()
    }

    private fun loadWords() {
        val user = SessionManager.getUser() ?: return
        binding.progressLoading.visibility = View.VISIBLE
        binding.tvGenerating.visibility = View.VISIBLE
        binding.cardWord.visibility = View.GONE

        lifecycleScope.launch {
            words = wordService.getWordsForLesson(user.id)

            if (words.isEmpty()) {
                words = wordService.getAnyWords(10)
            }

            total = words.size
            binding.progressLoading.visibility = View.GONE
            binding.tvGenerating.visibility = View.GONE

            if (words.isNotEmpty()) {
                binding.cardWord.visibility = View.VISIBLE
                showCurrentWord()
            } else {
                Toast.makeText(this@LessonActivity, "Нет слов для урока", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun showCurrentWord() {
        if (currentIndex >= words.size) return

        val word = words[currentIndex]
        binding.tvCurrentWord.text = word.word
        binding.tvScore.text = score.toString()
        binding.etAnswer.setText("")

        binding.layoutAnswer.visibility = View.VISIBLE
        binding.layoutFeedback.visibility = View.GONE
        binding.cardExample.visibility = View.GONE
        binding.btnShowExample.visibility = View.VISIBLE
        binding.tvGeneratingExample.visibility = View.GONE

        val progress = if (total > 0) ((currentIndex) * 100 / total) else 0
        binding.progressLesson.progress = progress
    }

    private fun checkAnswer() {
        val user = SessionManager.getUser() ?: return
        val word = words.getOrNull(currentIndex) ?: return
        val answer = binding.etAnswer.text.toString().trim()

        if (answer.isEmpty()) {
            Toast.makeText(this, "Введите ответ", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnCheck.isEnabled = false

        lifecycleScope.launch {
            val isCorrect = answer.equals(word.translation, ignoreCase = true)
            if (isCorrect) score++

            val feedback = aiService.checkAnswer(word.word, word.translation, answer)

            wordService.saveExerciseResult(user.id, word.id, isCorrect, answer, feedback)

            binding.tvFeedback.text = if (isCorrect) {
                "✅ $feedback"
            } else {
                "❌ $feedback"
            }
            binding.tvFeedback.setTextColor(
                if (isCorrect) getColor(com.linguaai.app.R.color.green)
                else getColor(com.linguaai.app.R.color.red)
            )

            binding.layoutAnswer.visibility = View.GONE
            binding.layoutFeedback.visibility = View.VISIBLE
            binding.btnShowExample.visibility = View.GONE
            binding.btnCheck.isEnabled = true

            binding.tvScore.text = score.toString()

            val isLast = currentIndex >= words.size - 1
            binding.btnNext.visibility = if (isLast) View.GONE else View.VISIBLE
            binding.btnEndLesson.visibility = if (isLast) View.VISIBLE else View.GONE
        }
    }

    private fun nextWord() {
        currentIndex++
        if (currentIndex < words.size) {
            showCurrentWord()
        }
    }

    private fun endLesson() {
        val user = SessionManager.getUser() ?: return

        lifecycleScope.launch {
            wordService.finishSession(user.id)
            val newAchievements = achievementService.checkAndAwardAchievements(user.id)

            if (newAchievements.isNotEmpty()) {
                val names = newAchievements.joinToString("\n") {
                    "${it.iconEmoji ?: "🏆"} ${it.name}"
                }
                AlertDialog.Builder(this@LessonActivity)
                    .setTitle("🎉 Новые достижения!")
                    .setMessage(names)
                    .setPositiveButton("Отлично!") { _, _ -> finish() }
                    .show()
            } else {
                Toast.makeText(
                    this@LessonActivity,
                    "Урок завершён! Счёт: $score/$total",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    private fun loadExample() {
        val word = words.getOrNull(currentIndex) ?: return
        binding.tvGeneratingExample.visibility = View.VISIBLE
        binding.btnShowExample.visibility = View.GONE

        lifecycleScope.launch {
            val example = if (!word.exampleSentence.isNullOrBlank()) {
                "${word.exampleSentence}\n${word.exampleTranslation ?: ""}"
            } else {
                aiService.generateExampleSentence(word.word, "английском")
            }

            binding.tvExample.text = example
            binding.cardExample.visibility = View.VISIBLE
            binding.tvGeneratingExample.visibility = View.GONE
        }
    }

    private fun abortLesson() {
        AlertDialog.Builder(this)
            .setTitle("Завершить урок?")
            .setMessage("Прогресс будет сохранён")
            .setPositiveButton("Да") { _, _ ->
                val user = SessionManager.getUser()
                if (user != null) {
                    lifecycleScope.launch {
                        wordService.finishSession(user.id)
                        finish()
                    }
                } else {
                    finish()
                }
            }
            .setNegativeButton("Нет", null)
            .show()
    }
}
