package com.example.clubdeportivo.ui.areas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.clubdeportivo.R
import com.example.clubdeportivo.databinding.FragmentAreasBinding

class AreasFragment : Fragment() {

    private var _binding: FragmentAreasBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AreasViewModel by viewModels()
    private lateinit var adapter: AreaAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAreasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AreaAdapter { area ->
            val destino = bundleOf(
                "areaId" to area.id,
                "areaNombre" to area.nombre
            )
            findNavController().navigate(R.id.action_areasFragment_to_reservarFragment, destino)
        }
        binding.recyclerAreas.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerAreas.adapter = adapter

        binding.swipeRefreshAreas.setOnRefreshListener { viewModel.cargarAreas() }

        viewModel.areas.observe(viewLifecycleOwner) { areas ->
            adapter.submitList(areas)
            binding.textAreasVacio.visibility = if (areas.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.cargando.observe(viewLifecycleOwner) { cargando ->
            binding.swipeRefreshAreas.isRefreshing = cargando
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
