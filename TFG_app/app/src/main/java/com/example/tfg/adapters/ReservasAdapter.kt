package com.example.tfg.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tfg.R
import com.example.tfg.models.Intercambio
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReservasAdapter(
    private var listaReservas: List<Intercambio>,
    private val currentUserId: Long,
    private val onItemClick: (Intercambio) -> Unit
) : RecyclerView.Adapter<ReservasAdapter.ReservaViewHolder>() {

    // Formato de fecha del backend: "2026-03-19 15:00:00"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

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

        // Determinar si está expirada: comprobación cliente-side si el backend aún no actualizó
        val estaExpirada = esExpiradaClientSide(reserva)
        val estadoEfectivo = if (estaExpirada) "EXPIRADA" else reserva.estadoIntercambio

        // Chip de Rol (VENTA / COMPRA)
        if (reserva.idVendedor == currentUserId) {
            holder.tvTipo.text = "VENTA"
            holder.tvTipo.setTextColor(Color.parseColor("#2E7D32")) // Verde
        } else {
            holder.tvTipo.text = "COMPRA"
            holder.tvTipo.setTextColor(Color.parseColor("#1976D2")) // Azul
        }

        // Chip de Estado (con tercer estado EXPIRADA en gris)
        when {
            estadoEfectivo.equals("EXPIRADA", ignoreCase = true) -> {
                holder.tvEstado.text = "Tiempo agotado"
                holder.tvEstado.setTextColor(Color.parseColor("#9E9E9E"))
            }
            estadoEfectivo.equals("Completado", ignoreCase = true) ||
            estadoEfectivo.equals("FINALIZADO", ignoreCase = true) -> {
                holder.tvEstado.text = "Finalizado ✓"
                holder.tvEstado.setTextColor(Color.parseColor("#2E7D32"))
            }
            estadoEfectivo.equals("Reservado", ignoreCase = true) -> {
                holder.tvEstado.text = "Reservado 🔒"
                holder.tvEstado.setTextColor(Color.parseColor("#1976D2"))
            }
            else -> {
                holder.tvEstado.text = estadoEfectivo
                holder.tvEstado.setTextColor(Color.parseColor("#FF8C00")) // Naranja para "Esperando"
            }
        }

        holder.tvPrecio.text = reserva.plazaDireccionTexto ?: "Ubicación sin concretar"
        holder.tvHora.text = reserva.momentoIntercambioPrevisto.replace("T", " ")

        holder.itemView.setOnClickListener {
            onItemClick(reserva)
        }
    }

    /**
     * Compara la fecha del intercambio (+ minutos de cortesía) con la fecha actual del sistema.
     * Si la hora de gracia ya pasó, es expirada aunque el backend aún no lo haya marcado.
     */
    private fun esExpiradaClientSide(reserva: Intercambio): Boolean {
        // Solo marcamos como expirada si el estado aún dice "Esperando" (no confirmada)
        if (!reserva.estadoIntercambio.equals("Esperando", ignoreCase = true)) return false

        return try {
            val fechaReserva = dateFormat.parse(reserva.momentoIntercambioPrevisto) ?: return false
            val cortesia = (reserva.cortesiaMinutos * 60 * 1000).toLong()
            val tiempoLimite = fechaReserva.time + cortesia
            System.currentTimeMillis() > tiempoLimite
        } catch (e: Exception) {
            false // Si no podemos parsear la fecha, no la expiraremos por seguridad
        }
    }

    override fun getItemCount(): Int = listaReservas.size

    fun actualizarDatos(nuevaLista: List<Intercambio>) {
        listaReservas = nuevaLista
        notifyDataSetChanged()
    }
}