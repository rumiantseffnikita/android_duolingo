package com.linguaai.app.ui.vocabulary

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.linguaai.app.databinding.ItemVocabularyGroupBinding
import com.linguaai.app.databinding.ItemVocabularyWordBinding

class VocabularyAdapter(
    private val groups: List<VocabularyGroup>
) : RecyclerView.Adapter<VocabularyAdapter.GroupViewHolder>() {

    class GroupViewHolder(val binding: ItemVocabularyGroupBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = ItemVocabularyGroupBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return GroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = groups[position]
        holder.binding.tvTopicEmoji.text = group.topicEmoji
        holder.binding.tvTopicName.text = group.topicName
        holder.binding.tvProgress.text = "${group.learnedCount}/${group.totalCount}"

        holder.binding.rvWords.layoutManager = LinearLayoutManager(holder.itemView.context)
        holder.binding.rvWords.adapter = WordsAdapter(group.words)
    }

    override fun getItemCount() = groups.size
}

class WordsAdapter(
    private val words: List<VocabularyWordItem>
) : RecyclerView.Adapter<WordsAdapter.WordViewHolder>() {

    class WordViewHolder(val binding: ItemVocabularyWordBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        val binding = ItemVocabularyWordBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return WordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        val item = words[position]
        holder.binding.tvStatusEmoji.text = item.statusEmoji
        holder.binding.tvWord.text = item.word
        holder.binding.tvTranslation.text = item.translation
        holder.binding.tvReviewDate.text = item.nextReviewText
    }

    override fun getItemCount() = words.size
}
