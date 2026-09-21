package com.connecthub.app.ui.chatlist

import android.graphics.drawable.GradientDrawable
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.connecthub.app.databinding.ItemConversationBinding
import com.connecthub.app.domain.model.ConversationPreview
import com.connecthub.app.ui.common.AvatarPalette
import java.util.Date

class ConversationAdapter(
    private val onClick: (ConversationPreview) -> Unit
) : ListAdapter<ConversationPreview, ConversationAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemConversationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemConversationBinding,
        private val onClick: (ConversationPreview) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(preview: ConversationPreview) {
            binding.textContactName.text = preview.contact.displayName
            binding.textAvatarInitial.text = AvatarPalette.initialFor(preview.contact.displayName)

            val background = binding.textAvatarInitial.background.mutate() as? GradientDrawable
            background?.setColor(AvatarPalette.colorFor(preview.contact.displayName))

            binding.textLastMessage.text = when {
                preview.lastMessage == null -> "Tap to start chatting"
                preview.lastMessageIsOwn -> "You: ${preview.lastMessage}"
                else -> preview.lastMessage
            }

            binding.textTimestamp.text = preview.lastMessageTimestampMillis?.let { millis ->
                DateFormat.format("HH:mm", Date(millis)).toString()
            } ?: ""

            binding.root.setOnClickListener { onClick(preview) }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<ConversationPreview>() {
        override fun areItemsTheSame(oldItem: ConversationPreview, newItem: ConversationPreview) =
            oldItem.conversationId == newItem.conversationId

        override fun areContentsTheSame(oldItem: ConversationPreview, newItem: ConversationPreview) =
            oldItem == newItem
    }
}
