package com.example.clubdeportivo.ui.perfil

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.clubdeportivo.R
import com.example.clubdeportivo.data.model.nombreLegible
import com.example.clubdeportivo.databinding.FragmentPerfilBinding

class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PerfilViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.usuario.observe(viewLifecycleOwner) { usuario ->
            binding.textPerfilNombre.text = usuario?.nombre ?: ""
            binding.textPerfilCorreo.text = usuario?.correo ?: ""
            binding.textPerfilRol.text = usuario?.rol?.nombreLegible() ?: ""
        }

        binding.buttonVerMembresia.setOnClickListener {
            findNavController().navigate(R.id.action_perfilFragment_to_membresiaFragment)
        }

        binding.buttonCerrarSesion.setOnClickListener {
            viewModel.cerrarSesion()
            findNavController().navigate(R.id.action_perfilFragment_to_loginFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
