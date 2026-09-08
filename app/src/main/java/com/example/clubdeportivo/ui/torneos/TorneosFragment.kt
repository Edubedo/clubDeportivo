package com.example.clubdeportivo.ui.torneos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.clubdeportivo.databinding.FragmentTorneosBinding

class TorneosFragment : Fragment() {

    private var _binding: FragmentTorneosBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TorneosViewModel by viewModels()
    private lateinit var adapter: TorneoAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTorneosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = TorneoAdapter { torneo -> viewModel.inscribirse(torneo) }
        binding.recyclerTorneos.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerTorneos.adapter = adapter

        viewModel.torneos.observe(viewLifecycleOwner) { adapter.submitList(it) }

        viewModel.cargando.observe(viewLifecycleOwner) { cargando ->
            binding.progressTorneos.visibility = if (cargando) View.VISIBLE else View.GONE
        }

        viewModel.mensaje.observe(viewLifecycleOwner) { mensaje ->
            if (mensaje != null) {
                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
                viewModel.onMensajeMostrado()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
