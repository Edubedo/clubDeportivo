package com.example.clubdeportivo.ui.reservas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.clubdeportivo.databinding.FragmentMisReservasBinding

class MisReservasFragment : Fragment() {

    private var _binding: FragmentMisReservasBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MisReservasViewModel by viewModels()
    private val adapter = ReservaAdapter(onCancelarClick = { reserva -> viewModel.cancelar(reserva) })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMisReservasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerMisReservas.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerMisReservas.adapter = adapter

        viewModel.reservas.observe(viewLifecycleOwner) { reservas ->
            adapter.submitList(reservas)
            binding.textMisReservasVacio.visibility = if (reservas.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.cargando.observe(viewLifecycleOwner) { cargando ->
            binding.progressMisReservas.visibility = if (cargando) View.VISIBLE else View.GONE
        }

        viewModel.mensajeCancelacion.observe(viewLifecycleOwner) { mensaje ->
            if (mensaje != null) {
                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show()
                viewModel.onMensajeCancelacionMostrado()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.cargarReservas()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
