package com.example.clubdeportivo.ui.torneos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.clubdeportivo.data.model.Torneo
import com.example.clubdeportivo.databinding.ItemTorneoBinding

class TorneoAdapter(
    private val onInscribirseClick: (Torneo) -> Unit
) : ListAdapter<Torneo, TorneoAdapter.TorneoViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TorneoViewHolder {
        val binding = ItemTorneoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TorneoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TorneoViewHolder, position: Int) {
        holder.bind(getItem(position), onInscribirseClick)
    }

    class TorneoViewHolder(private val binding: ItemTorneoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(torneo: Torneo, onInscribirseClick: (Torneo) -> Unit) {
            binding.textTorneoNombre.text = torneo.nombre
            binding.textTorneoFechas.text = "${torneo.disciplina} · ${torneo.fechaInicio} a ${torneo.fechaFin}"
            binding.textTorneoCupo.text = "Cupo: ${torneo.inscritos} / ${torneo.cupoMaximo}"

            val cupoLleno = torneo.inscritos >= torneo.cupoMaximo
            binding.buttonInscribirse.isEnabled = !cupoLleno
            binding.buttonInscribirse.text = if (cupoLleno) "Cupo lleno" else "Inscribirme"
            binding.buttonInscribirse.setOnClickListener { onInscribirseClick(torneo) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Torneo>() {
        override fun areItemsTheSame(oldItem: Torneo, newItem: Torneo) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Torneo, newItem: Torneo) = oldItem == newItem
    }
}
