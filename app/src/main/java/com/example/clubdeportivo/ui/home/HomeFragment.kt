package com.example.clubdeportivo.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.clubdeportivo.R
import com.example.clubdeportivo.data.model.nombreLegible
import com.example.clubdeportivo.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.usuario.observe(viewLifecycleOwner) { usuario ->
            binding.textBienvenida.text = "¡Hola, ${usuario?.nombre ?: "socio"}!"

            binding.textRol.text = usuario?.rol?.nombreLegible() ?: ""
        }

        binding.buttonIrAreas.setOnClickListener {
            findNavController().navigate(R.id.areasFragment)
        }
        binding.buttonIrReservas.setOnClickListener {
            findNavController().navigate(R.id.misReservasFragment)
        }
        binding.buttonIrTorneos.setOnClickListener {
            findNavController().navigate(R.id.torneosFragment)
        }
        binding.buttonIrMembresia.setOnClickListener {
            findNavController().navigate(R.id.membresiaFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
