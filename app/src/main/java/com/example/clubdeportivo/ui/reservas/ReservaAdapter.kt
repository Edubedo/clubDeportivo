package com.example.clubdeportivo.ui.reservas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.clubdeportivo.data.model.EstadoReserva
import com.example.clubdeportivo.databinding.ItemReservaBinding

data class ReservaConNombreArea(
    val reservaId: Int,
    val nombreArea: String,
    val fecha: String,
    val horaInicio: String,
    val horaFin: String,
    val estado: EstadoReserva
)

class ReservaAdapter(
    private val onCancelarClick: (ReservaConNombreArea) -> Unit
) : ListAdapter<ReservaConNombreArea, ReservaAdapter.ReservaViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReservaViewHolder {
        val binding = ItemReservaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReservaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReservaViewHolder, position: Int) {
        holder.bind(getItem(position), onCancelarClick)
    }

    class ReservaViewHolder(private val binding: ItemReservaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(reserva: ReservaConNombreArea, onCancelarClick: (ReservaConNombreArea) -> Unit) {
            binding.textReservaArea.text = reserva.nombreArea
            binding.textReservaFechaHora.text = "${reserva.fecha} · ${reserva.horaInicio} - ${reserva.horaFin}"
            binding.textReservaEstado.text = reserva.estado.name

            val cancelable = reserva.estado == EstadoReserva.CONFIRMADA ||
                reserva.estado == EstadoReserva.PENDIENTE_APROBACION
            binding.buttonCancelarReserva.visibility = if (cancelable) View.VISIBLE else View.GONE
            binding.buttonCancelarReserva.setOnClickListener { onCancelarClick(reserva) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ReservaConNombreArea>() {
        override fun areItemsTheSame(oldItem: ReservaConNombreArea, newItem: ReservaConNombreArea) =
            oldItem.reservaId == newItem.reservaId
        override fun areContentsTheSame(oldItem: ReservaConNombreArea, newItem: ReservaConNombreArea) =
            oldItem == newItem
    }
}
