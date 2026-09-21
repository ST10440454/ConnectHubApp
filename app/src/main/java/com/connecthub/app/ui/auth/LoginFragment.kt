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
import com.connecthub.app.databinding.FragmentLoginBinding
import com.connecthub.app.ui.ViewModelFactory
import com.connecthub.app.ui.common.addTextChangedListener
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels {
        ViewModelFactory((requireActivity().application as ConnectHubApp).container)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.editEmail.addTextChangedListener {
            viewModel.onIntent(AuthIntent.EmailChanged(it))
        }
        binding.editPassword.addTextChangedListener {
            viewModel.onIntent(AuthIntent.PasswordChanged(it))
        }
        binding.buttonLogin.setOnClickListener {
            viewModel.onIntent(AuthIntent.SubmitLogin)
        }
        binding.buttonGoToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collect { state ->
                        binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                        binding.buttonLogin.isEnabled = !state.isLoading
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
                            findNavController().navigate(R.id.action_login_to_chatList)
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
