package com.connecthub.app.presentation.newchat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.connecthub.app.ConnectHubApp
import com.connecthub.app.R
import com.connecthub.app.databinding.FragmentNewChatBinding
import com.connecthub.app.domain.model.DeviceContactMatch
import com.connecthub.app.domain.model.conversationIdFor
import com.connecthub.app.presentation.ViewModelFactory
import kotlinx.coroutines.launch

class NewChatFragment : Fragment() {

    private var _binding: FragmentNewChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NewChatViewModel by viewModels {
        ViewModelFactory((requireActivity().application as ConnectHubApp).container)
    }

    private lateinit var adapter: DeviceContactAdapter

    private val requestContactsPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.onPermissionGranted() else viewModel.onPermissionDenied()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        adapter = DeviceContactAdapter(
            onChatClick = ::openChatWith,
            onInviteClick = ::inviteContact
        )
        binding.recyclerDeviceContacts.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerDeviceContacts.adapter = adapter

        binding.buttonOpenSettings.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", requireContext().packageName, null)
            }
            startActivity(intent)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    binding.permissionDeniedState.visibility = if (state.permissionDenied) View.VISIBLE else View.GONE
                    binding.recyclerDeviceContacts.visibility =
                        if (!state.permissionDenied && !state.isLoading) View.VISIBLE else View.GONE
                    adapter.submitList(state.matches)
                }
            }
        }

        checkPermissionAndLoad()
    }

    private fun checkPermissionAndLoad() {
        val alreadyGranted = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (alreadyGranted) {
            viewModel.onPermissionGranted()
        } else {
            requestContactsPermission.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    private fun openChatWith(match: DeviceContactMatch) {
        val contact = match.matchedContact ?: return
        // Reuse the exact same conversationId scheme as the Chats List, so opening
        // a chat from here or from an existing preview always lands on the same thread.
        val conversationId = conversationIdFor(requireCurrentUserId(), contact.uid)
        val args = Bundle().apply {
            putString("conversationId", conversationId)
            putString("contactId", contact.uid)
            putString("contactName", contact.displayName)
        }
        findNavController().navigate(R.id.action_newChat_to_chat, args)
    }

    private fun requireCurrentUserId(): String {
        return (requireActivity().application as ConnectHubApp).container
            .authRepository.currentUser()?.uid.orEmpty()
    }

    private fun inviteContact(match: DeviceContactMatch) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:${match.deviceContact.phoneNumber}")
            putExtra("sms_body", getString(R.string.invite_message_body))
        }
        if (intent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
