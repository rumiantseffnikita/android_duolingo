package com.linguaai.app.ui.achievements

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.linguaai.app.R
import com.linguaai.app.databinding.ItemAchievementBinding
import com.linguaai.app.services.AchievementService

class AchievementsAdapter(
    private val items: List<AchievementService.AchievementWithStatus>
) : RecyclerView.Adapter<AchievementsAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemAchievementBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAchievementBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val achievement = item.achievement

        holder.binding.tvEmoji.text = achievement.iconEmoji ?: "🏆"
        holder.binding.tvName.text = achievement.name
        holder.binding.tvDescription.text = achievement.description ?: ""

        if (item.isEarned) {
            holder.binding.tvStatus.text = "✅ Получено ${item.earnedAt?.take(10) ?: ""}"
            holder.binding.tvStatus.setTextColor(
                holder.itemView.context.getColor(R.color.green)
            )
            holder.itemView.alpha = 1.0f
        } else {
            holder.binding.tvStatus.text = "Не получено"
            holder.binding.tvStatus.setTextColor(
                holder.itemView.context.getColor(R.color.text_hint)
            )
            holder.itemView.alpha = 0.5f
        }
    }

    override fun getItemCount() = items.size
}
