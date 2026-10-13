package com.connecthub.app.presentation.newchat

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.connecthub.app.R
import com.connecthub.app.databinding.ItemDeviceContactBinding
import com.connecthub.app.domain.model.DeviceContactMatch
import com.connecthub.app.presentation.common.AvatarPalette

class DeviceContactAdapter(
    private val onChatClick: (DeviceContactMatch) -> Unit,
    private val onInviteClick: (DeviceContactMatch) -> Unit
) : ListAdapter<DeviceContactMatch, DeviceContactAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDeviceContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onChatClick, onInviteClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemDeviceContactBinding,
        private val onChatClick: (DeviceContactMatch) -> Unit,
        private val onInviteClick: (DeviceContactMatch) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(match: DeviceContactMatch) {
            val name = match.matchedContact?.displayName ?: match.deviceContact.name
            binding.textContactName.text = name
            binding.textAvatarInitial.text = AvatarPalette.initialFor(name)
            val background = binding.textAvatarInitial.background.mutate() as? GradientDrawable
            background?.setColor(AvatarPalette.colorFor(name))

            if (match.matchedContact != null) {
                binding.textStatus.text = binding.root.context.getString(R.string.on_connecthub)
                binding.buttonAction.text = binding.root.context.getString(R.string.action_chat)
                binding.buttonAction.setOnClickListener { onChatClick(match) }
            } else {
                binding.textStatus.text = match.deviceContact.phoneNumber
                binding.buttonAction.text = binding.root.context.getString(R.string.action_invite)
                binding.buttonAction.setOnClickListener { onInviteClick(match) }
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<DeviceContactMatch>() {
        override fun areItemsTheSame(oldItem: DeviceContactMatch, newItem: DeviceContactMatch) =
            oldItem.deviceContact.phoneNumber == newItem.deviceContact.phoneNumber

        override fun areContentsTheSame(oldItem: DeviceContactMatch, newItem: DeviceContactMatch) =
            oldItem == newItem
    }
}
