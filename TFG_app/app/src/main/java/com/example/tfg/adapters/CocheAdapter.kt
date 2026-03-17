package com.example.tfg.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.tfg.databinding.ItemCocheBinding
import com.example.tfg.models.Coche

class CocheAdapter(
    private var listaCoches: MutableList<Coche>,
    private val onEliminarClick: (Coche) -> Unit // Añadimos este parámetro al constructor
) : RecyclerView.Adapter<CocheAdapter.CocheViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CocheViewHolder {
        val binding = ItemCocheBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CocheViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CocheViewHolder, position: Int) {
        val coche = listaCoches[position]
        holder.bind(coche)
    }

    override fun getItemCount(): Int = listaCoches.size

    inner class CocheViewHolder(private val binding: ItemCocheBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(coche: Coche) {
            val colorTexto = if (!coche.color.isNullOrBlank() && coche.color != "No especificado") " (${coche.color})" else ""
            binding.tvMarcaModelo.text = "${coche.marca ?: "Marca"} ${coche.modelo ?: "Modelo"}$colorTexto"
            binding.tvMatricula.text = coche.matricula ?: "Sin matrícula"

            // Configuramos el clic del botón eliminar que definimos en el XML
            binding.btnEliminarCoche.setOnClickListener {
                onEliminarClick(coche) // Llamamos a la función que viene del Fragment
            }
        }
    }

    fun actualizarCoches(nuevaLista: List<Coche>) {
        // Rompemos el enlace de memoria haciendo una fotocopia
        this.listaCoches = nuevaLista.toMutableList()
        notifyDataSetChanged()
    }
}