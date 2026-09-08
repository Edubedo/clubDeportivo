package com.example.clubdeportivo.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.clubdeportivo.R
import com.example.clubdeportivo.databinding.FragmentLoginBinding

/**
 * Vista de la pantalla de login. Solo muestra datos y reenvía acciones del
 * usuario al ViewModel: no valida ni decide nada por sí misma.
 */
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonLogin.setOnClickListener {
            val email = binding.editEmail.text.toString()
            val password = binding.editPassword.text.toString()
            viewModel.login(email, password)
        }

        observarViewModel()
    }

    private fun observarViewModel() {
        viewModel.emailError.observe(viewLifecycleOwner) { error ->
            binding.emailLayout.error = error
        }

        viewModel.passwordError.observe(viewLifecycleOwner) { error ->
            binding.passwordLayout.error = error
        }

        viewModel.cargando.observe(viewLifecycleOwner) { cargando ->
            binding.progressLogin.visibility = if (cargando) View.VISIBLE else View.GONE
            binding.buttonLogin.isEnabled = !cargando
        }

        viewModel.errorGeneral.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.loginExitoso.observe(viewLifecycleOwner) { exitoso ->
            if (exitoso) {
                findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                viewModel.onNavegacionCompletada()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
