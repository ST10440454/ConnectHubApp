package com.connecthub.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.connecthub.app.ConnectHubApp
import com.connecthub.app.R
import com.connecthub.app.databinding.FragmentRegisterBinding
import com.connecthub.app.ui.ViewModelFactory
import com.connecthub.app.ui.common.addTextChangedListener
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    // Note: this fragment gets its OWN AuthViewModel instance (fragment-scoped),
    // separate from LoginFragment's — they don't share state, which is correct
    // since register and login are different forms.
    private val viewModel: AuthViewModel by viewModels {
        ViewModelFactory((requireActivity().application as ConnectHubApp).container)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.editName.addTextChangedListener {
            viewModel.onIntent(AuthIntent.NameChanged(it))
        }
        binding.editEmail.addTextChangedListener {
            viewModel.onIntent(AuthIntent.EmailChanged(it))
        }
        binding.editPassword.addTextChangedListener {
            viewModel.onIntent(AuthIntent.PasswordChanged(it))
        }
        binding.buttonRegister.setOnClickListener {
            viewModel.onIntent(AuthIntent.SubmitRegister)
        }
        binding.buttonGoToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collect { state ->
                        binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                        binding.buttonRegister.isEnabled = !state.isLoading
                        if (state.errorMessage != null) {
                            binding.textError.text = state.errorMessage
                            binding.textError.visibility = View.VISIBLE
                        } else {
                            binding.textError.visibility = View.GONE
                        }
                    }
                }
                launch {
                    viewModel.effects.collect { effect ->
                        if (effect is AuthEffect.NavigateToChat) {
                            findNavController().navigate(R.id.action_register_to_chatList)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
