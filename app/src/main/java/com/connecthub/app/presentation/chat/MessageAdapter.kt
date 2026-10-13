package com.connecthub.app.presentation.chat

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.connecthub.app.R
import com.connecthub.app.databinding.ItemMessageReceivedBinding
import com.connecthub.app.databinding.ItemMessageSentBinding
import com.connecthub.app.domain.model.Message
import com.connecthub.app.domain.model.MessageStatus
import com.connecthub.app.domain.model.MessageType
import java.util.Date

private const val VIEW_TYPE_SENT = 1
private const val VIEW_TYPE_RECEIVED = 2

class MessageAdapter(
    private val currentUserId: String?
) : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).senderId == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_SENT) {
            SentViewHolder(ItemMessageSentBinding.inflate(inflater, parent, false))
        } else {
            ReceivedViewHolder(ItemMessageReceivedBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is SentViewHolder -> holder.bind(message)
            is ReceivedViewHolder -> holder.bind(message)
        }
    }

    class SentViewHolder(private val binding: ItemMessageSentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            if (message.type == MessageType.IMAGE && message.imageUrl != null) {
                binding.imageContent.visibility = View.VISIBLE
                binding.imageContent.load(message.imageUrl) {
                    placeholder(R.drawable.bg_bubble_received)
                }
                binding.textContent.visibility = View.GONE
            } else {
                binding.imageContent.visibility = View.GONE
                binding.textContent.visibility = View.VISIBLE
                binding.textContent.text = message.content
            }
            binding.textTimestamp.text = DateFormat.format("HH:mm", Date(message.timestampMillis))
            binding.imageStatus.setImageResource(
                when (message.status) {
                    MessageStatus.SENT -> R.drawable.ic_check_sent
                    MessageStatus.DELIVERED -> R.drawable.ic_check_delivered
                    MessageStatus.READ -> R.drawable.ic_check_read
                }
            )
        }
    }

    class ReceivedViewHolder(private val binding: ItemMessageReceivedBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            if (message.type == MessageType.IMAGE && message.imageUrl != null) {
                binding.imageContent.visibility = View.VISIBLE
                binding.imageContent.load(message.imageUrl) {
                    placeholder(R.drawable.bg_bubble_sent)
                }
                binding.textContent.visibility = View.GONE
            } else {
                binding.imageContent.visibility = View.GONE
                binding.textContent.visibility = View.VISIBLE
                binding.textContent.text = message.content
            }
            binding.textSenderName.text = message.senderName
            binding.textTimestamp.text = DateFormat.format("HH:mm", Date(message.timestampMillis))
        }
    }
}

private class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
    override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean =
        oldItem == newItem
}
