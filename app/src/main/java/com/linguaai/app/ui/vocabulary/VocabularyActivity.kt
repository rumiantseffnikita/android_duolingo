package com.linguaai.app.ui.vocabulary

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.linguaai.app.databinding.ActivityVocabularyBinding
import com.linguaai.app.services.WordService
import com.linguaai.app.utils.SessionManager
import kotlinx.coroutines.launch

class VocabularyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVocabularyBinding
    private val wordService = WordService()
    private var allGroups = listOf<VocabularyGroup>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVocabularyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.rvVocabulary.layoutManager = LinearLayoutManager(this)

        binding.etSearch.addTextChangedListener { text ->
            filterGroups(text?.toString() ?: "")
        }

        loadVocabulary()
    }

    private fun loadVocabulary() {
        val user = SessionManager.getUser() ?: return
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val raw = wordService.getVocabulary(user.id)

            allGroups = raw.map { (topic, items) ->
                VocabularyGroup(
                    topicName = topic?.name ?: "Без темы",
                    topicEmoji = topic?.iconEmoji ?: "📝",
                    words = items.map { item ->
                        VocabularyWordItem(
                            word = item.word.word,
                            translation = item.word.translation,
                            isLearned = item.progress?.isLearned == true,
                            correctCount = item.progress?.correctCount ?: 0,
                            wrongCount = item.progress?.wrongCount ?: 0,
                            nextReview = item.progress?.nextReview,
                            hasProgress = item.progress != null
                        )
                    }
                )
            }

            binding.rvVocabulary.adapter = VocabularyAdapter(allGroups)
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun filterGroups(query: String) {
        if (query.isBlank()) {
            binding.rvVocabulary.adapter = VocabularyAdapter(allGroups)
            return
        }

        val q = query.trim().lowercase()
        val filtered = allGroups.mapNotNull { group ->
            val filteredWords = group.words.filter {
                it.word.lowercase().contains(q) || it.translation.lowercase().contains(q)
            }
            if (filteredWords.isNotEmpty()) {
                group.copy(words = filteredWords)
            } else null
        }

        binding.rvVocabulary.adapter = VocabularyAdapter(filtered)
    }
}

data class VocabularyGroup(
    val topicName: String,
    val topicEmoji: String,
    val words: List<VocabularyWordItem>
) {
    val learnedCount get() = words.count { it.isLearned }
    val totalCount get() = words.size
}

data class VocabularyWordItem(
    val word: String,
    val translation: String,
    val isLearned: Boolean,
    val correctCount: Int,
    val wrongCount: Int,
    val nextReview: String?,
    val hasProgress: Boolean
) {
    val statusEmoji get() = when {
        isLearned -> "✅"
        hasProgress -> "📖"
        else -> "🆕"
    }
    val nextReviewText get() = if (nextReview != null) "Повтор: ${nextReview.take(5)}" else "Новое"
}
