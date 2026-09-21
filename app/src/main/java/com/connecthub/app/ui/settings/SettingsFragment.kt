package com.connecthub.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.connecthub.app.ConnectHubApp
import com.connecthub.app.R
import com.connecthub.app.databinding.FragmentSettingsBinding
import com.connecthub.app.domain.model.SupportedLanguages
import com.connecthub.app.ui.ViewModelFactory
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels {
        ViewModelFactory((requireActivity().application as ConnectHubApp).container)
    }

    // Guards against switch callbacks firing when we set their value
    // programmatically from a state update (would otherwise create a feedback
    // loop with the DataStore write).
    private var isApplyingState = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isApplyingState) return@setOnCheckedChangeListener
            viewModel.onIntent(SettingsIntent.ToggleDarkMode(isChecked))
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (isApplyingState) return@setOnCheckedChangeListener
            viewModel.onIntent(SettingsIntent.ToggleNotifications(isChecked))
        }

        setUpLanguageDropdown()

        binding.buttonLogout.setOnClickListener {
            viewModel.onIntent(SettingsIntent.LogOut)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    isApplyingState = true
                    binding.switchDarkMode.isChecked = state.settings.darkModeEnabled
                    binding.switchNotifications.isChecked = state.settings.notificationsEnabled
                    isApplyingState = false

                    binding.textUserEmail.text = state.userEmail?.let {
                        getString(R.string.signed_in_as, it)
                    } ?: ""

                    if (state.loggedOut) {
                        findNavController().navigate(R.id.action_settings_to_login)
                    }
                }
            }
        }
    }

    /**
     * Language selection is a static, system-level concern rather than app state:
     * AppCompatDelegate.setApplicationLocales() applies the chosen locale immediately
     * (recreating this Fragment/Activity to pick up the matching values-<code>/strings.xml)
     * and persists it automatically across app restarts — no DataStore write needed here.
     */
    private fun setUpLanguageDropdown() {
        val languages = SupportedLanguages.ALL
        val names = languages.map { it.displayName }
        binding.dropdownLanguage.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, names)
        )

        val currentTag = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore(",")
        val currentLanguage = languages.firstOrNull { it.code == currentTag } ?: languages.first()
        binding.dropdownLanguage.setText(currentLanguage.displayName, false)

        binding.dropdownLanguage.setOnItemClickListener { _, _, position, _ ->
            val selected = languages.getOrNull(position) ?: return@setOnItemClickListener
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(selected.code))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
