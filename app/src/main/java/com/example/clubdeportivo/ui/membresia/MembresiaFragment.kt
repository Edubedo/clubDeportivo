package com.example.clubdeportivo.ui.membresia

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.clubdeportivo.databinding.FragmentMembresiaBinding

class MembresiaFragment : Fragment() {

    private var _binding: FragmentMembresiaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MembresiaViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMembresiaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.cargando.observe(viewLifecycleOwner) { cargando ->
            binding.progressMembresia.visibility = if (cargando) View.VISIBLE else View.GONE
        }

        viewModel.titulo.observe(viewLifecycleOwner) { binding.textMembresiaTipo.text = it }
        viewModel.mensaje.observe(viewLifecycleOwner) { binding.textMembresiaEstado.text = it ?: "" }
        viewModel.detalle.observe(viewLifecycleOwner) { binding.textMembresiaVigencia.text = it ?: "" }

        viewModel.beneficios.observe(viewLifecycleOwner) { beneficios ->
            val hayBeneficios = beneficios.isNotEmpty()
            binding.textMembresiaBeneficiosTitulo.visibility = if (hayBeneficios) View.VISIBLE else View.GONE
            binding.textMembresiaBeneficios.visibility = if (hayBeneficios) View.VISIBLE else View.GONE
            binding.textMembresiaBeneficios.text = beneficios.joinToString("\n") { "• $it" }
        }

        viewModel.integrantes.observe(viewLifecycleOwner) { integrantes ->
            val hayIntegrantes = integrantes.isNotEmpty()
            binding.textMembresiaIntegrantesTitulo.visibility = if (hayIntegrantes) View.VISIBLE else View.GONE
            binding.textMembresiaIntegrantes.visibility = if (hayIntegrantes) View.VISIBLE else View.GONE
            binding.textMembresiaIntegrantes.text = integrantes.joinToString("\n") {
                "• ${it.nombre} (${it.parentesco})"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
