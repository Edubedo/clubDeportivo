package com.example.clubdeportivo.ui.areas

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.clubdeportivo.R
import com.example.clubdeportivo.data.model.Area
import com.example.clubdeportivo.data.model.DisponibilidadArea
import com.example.clubdeportivo.databinding.ItemAreaBinding

/**
 * Pinta cada área como una tarjeta de color, imitando el "mapa tipo cine":
 * azul = disponible, rojo = ocupada, gris = en mantenimiento. Estos colores
 * son fijos (no cambian con el tema claro/oscuro): son "chips" de estado,
 * por eso el texto también se fija en un tono oscuro que siempre contraste.
 */
class AreaAdapter(
    private val onAreaClick: (Area) -> Unit
) : ListAdapter<Area, AreaAdapter.AreaViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AreaViewHolder {
        val binding = ItemAreaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AreaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AreaViewHolder, position: Int) {
        holder.bind(getItem(position), onAreaClick)
    }

    class AreaViewHolder(private val binding: ItemAreaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(area: Area, onAreaClick: (Area) -> Unit) {
            binding.textAreaNombre.text = area.nombre
            binding.textAreaTipo.text = "${area.tipo} · ${area.capacidad} personas"

            val contexto = binding.root.context
            val (colorFondoRes, colorTextoRes, texto) = when (area.disponibilidad) {
                DisponibilidadArea.DISPONIBLE ->
                    Triple(R.color.area_disponible_bg, R.color.area_disponible_text, "DISPONIBLE")
                DisponibilidadArea.OCUPADA ->
                    Triple(R.color.area_ocupada_bg, R.color.area_ocupada_text, "OCUPADA")
                DisponibilidadArea.MANTENIMIENTO ->
                    Triple(R.color.area_mantenimiento_bg, R.color.area_mantenimiento_text, "MANTENIMIENTO")
            }
            val colorTexto = ContextCompat.getColor(contexto, colorTextoRes)

            binding.layoutAreaContenido.setBackgroundColor(ContextCompat.getColor(contexto, colorFondoRes))
            binding.textAreaEstado.text = texto
            binding.textAreaEstado.setTextColor(colorTexto)
            binding.textAreaNombre.setTextColor(colorTexto)
            binding.textAreaTipo.setTextColor(colorTexto)

            binding.root.isEnabled = area.disponibilidad == DisponibilidadArea.DISPONIBLE
            binding.root.setOnClickListener {
                if (area.disponibilidad == DisponibilidadArea.DISPONIBLE) onAreaClick(area)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Area>() {
        override fun areItemsTheSame(oldItem: Area, newItem: Area) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Area, newItem: Area) = oldItem == newItem
    }
}
