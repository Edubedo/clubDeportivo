package com.example.clubdeportivo.ui.reservar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.RadioButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.clubdeportivo.databinding.FragmentReservarBinding
import com.example.clubdeportivo.util.Fechas

class ReservarFragment : Fragment() {

    private var _binding: FragmentReservarBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReservarViewModel by viewModels()

    /** Índice del spinner -> fecha "yyyy-MM-dd" real de ese día. */
    private val dias = Fechas.proximosDias(8)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReservarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val areaId = requireArguments().getInt("areaId")
        val areaNombre = requireArguments().getString("areaNombre").orEmpty()
        binding.textReservarArea.text = areaNombre

        binding.spinnerDia.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            dias.map { it.second }
        )
        binding.spinnerDia.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: View?, position: Int, id: Long) {
                viewModel.seleccionarDia(dias[position].first)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }

        binding.buttonConfirmarReserva.setOnClickListener {
            val idSeleccionado = binding.radioGroupHorarios.checkedRadioButtonId
            if (idSeleccionado == -1) return@setOnClickListener
            val radioSeleccionado = binding.root.findViewById<RadioButton>(idSeleccionado)
            val posicionDia = binding.spinnerDia.selectedItemPosition
            viewModel.reservar(dias[posicionDia].first, radioSeleccionado.tag.toString())
        }

        viewModel.cargarArea(areaId)
        observarViewModel()
    }

    private fun observarViewModel() {
        viewModel.bloqueadoParaExterno.observe(viewLifecycleOwner) { bloqueado ->
            binding.textBloqueadoExterno.visibility = if (bloqueado) View.VISIBLE else View.GONE
            binding.grupoFormularioReserva.visibility = if (bloqueado) View.GONE else View.VISIBLE
        }

        viewModel.horarioTexto.observe(viewLifecycleOwner) { texto ->
            binding.textReservarHorarioArea.text = texto
        }

        viewModel.slots.observe(viewLifecycleOwner) { slots ->
            binding.radioGroupHorarios.removeAllViews()
            slots.forEachIndexed { index, hora ->
                val radioButton = RadioButton(requireContext()).apply {
                    id = View.generateViewId()
                    text = "$hora - ${"%02d:00".format(hora.substring(0, 2).toInt() + 1)}"
                    tag = hora
                    isChecked = index == 0
                }
                binding.radioGroupHorarios.addView(radioButton)
            }
            binding.textSinHorarios.visibility = if (slots.isEmpty()) View.VISIBLE else View.GONE
            binding.buttonConfirmarReserva.isEnabled = slots.isNotEmpty()
        }

        viewModel.cargando.observe(viewLifecycleOwner) { cargando ->
            binding.progressReservar.visibility = if (cargando) View.VISIBLE else View.GONE
            binding.buttonConfirmarReserva.isEnabled = !cargando && viewModel.slots.value?.isNotEmpty() == true
        }

        viewModel.errorReserva.observe(viewLifecycleOwner) { error ->
            binding.textErrorReserva.visibility = if (error != null) View.VISIBLE else View.GONE
            binding.textErrorReserva.text = error
        }

        viewModel.reservaConfirmada.observe(viewLifecycleOwner) { reserva ->
            if (reserva == null) return@observe
            val cantidadMaterial = viewModel.materialAsignado.value?.sumOf { it.cantidad } ?: 0
            val detalleMaterial = when {
                reserva.esExterno -> "\nTu reservación quedó PENDIENTE DE APROBACIÓN por un administrador."
                cantidadMaterial > 0 -> "\nMaterial asignado automáticamente: $cantidadMaterial unidad(es)."
                else -> "\nEsta área no requiere material adicional."
            }
            binding.textResultadoReserva.text =
                "Reserva para el ${reserva.fecha} de ${reserva.horaInicio} a ${reserva.horaFin}.$detalleMaterial"
            binding.cardResultado.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
