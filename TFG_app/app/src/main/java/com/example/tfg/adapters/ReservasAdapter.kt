package com.example.tfg.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tfg.R
import com.example.tfg.models.Intercambio

class ReservasAdapter(
    private var listaReservas: List<Intercambio>,
    private val currentUserId: Long, // Pasamos tu ID para saber si vendes o compras
    private val onItemClick: (Intercambio) -> Unit // Para cuando pulses en la reserva
) : RecyclerView.Adapter<ReservasAdapter.ReservaViewHolder>() {

    class ReservaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTipo: TextView = view.findViewById(R.id.tvTipoReserva)
        val tvEstado: TextView = view.findViewById(R.id.tvEstadoReserva)
        val tvPrecio: TextView = view.findViewById(R.id.tvPrecioReserva)
        val tvHora: TextView = view.findViewById(R.id.tvHoraReserva)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReservaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reserva, parent, false)
        return ReservaViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReservaViewHolder, position: Int) {
        val reserva = listaReservas[position]

        // LÓGICA UBER: ¿Soy el que vende o el que compra?
        if (reserva.idVendedor == currentUserId) {
            holder.tvTipo.text = "OFRECIENDO"
            holder.tvTipo.setTextColor(Color.parseColor("#FF8C00")) // Naranja para ventas
        } else {
            holder.tvTipo.text = "COMPRANDO"
            holder.tvTipo.setTextColor(Color.parseColor("#2E7D32")) // Verde para compras
        }

        // Pintamos el resto de datos
        holder.tvEstado.text = reserva.estadoIntercambio
        holder.tvPrecio.text = "${"%.2f".format(reserva.precioTotalComprador)}€"

        if (reserva.momentoIntercambioPrevisto.length >= 16) {
            holder.tvHora.text = "Salida: ${reserva.momentoIntercambioPrevisto.substring(11, 16)}"
        } else {
            holder.tvHora.text = "Salida: --:--"
        }

        // Detectar el clic en la tarjeta (para ir luego al "Radar" o al "Viaje")
        holder.itemView.setOnClickListener {
            onItemClick(reserva)
        }
    }

    override fun getItemCount(): Int = listaReservas.size

    // Función para refrescar la lista cuando el servidor nos responda
    fun actualizarDatos(nuevaLista: List<Intercambio>) {
        listaReservas = nuevaLista
        notifyDataSetChanged()
    }
}