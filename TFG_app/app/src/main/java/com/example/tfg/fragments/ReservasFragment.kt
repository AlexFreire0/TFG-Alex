package com.example.tfg.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tfg.R
import com.example.tfg.adapters.ReservasAdapter
import com.example.tfg.models.Intercambio
import com.example.tfg.models.Usuario
import com.example.tfg.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.content.Intent
import kotlin.jvm.java
import com.example.tfg.activities.*

class ReservasFragment : Fragment() {

    private lateinit var rvReservas: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var adapter: ReservasAdapter
    private var listaCombinada: MutableList<Intercambio> = mutableListOf()
    private var usuarioLogueado: Usuario? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Asegúrate de que este layout coincide con tu XML (fragment_reservas.xml)
        val view = inflater.inflate(R.layout.fragment_reservas, container, false)

        rvReservas = view.findViewById(R.id.rvMisReservas)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)

        // Intentamos recuperar el usuario de la Activity contenedora
        usuarioLogueado = requireActivity().intent.getParcelableExtra("USER_DATA")
        val userId = usuarioLogueado?.uid ?: 0L

        // Inicializamos el Adapter vacío al principio
// Cambia esto en el onCreateView de tu ReservasFragment
        adapter = ReservasAdapter(emptyList(), userId) { reservaClicada ->
            if (reservaClicada.estadoIntercambio == "Reservado") {
                val intent = Intent(requireContext(), RadarActivity::class.java)
                intent.putExtra("INTERCAMBIO_DATA", reservaClicada)
                intent.putExtra("USER_DATA", usuarioLogueado) // Pasamos el usuario también
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "Estado: ${reservaClicada.estadoIntercambio}", Toast.LENGTH_SHORT).show()
            }
        }

        rvReservas.layoutManager = LinearLayoutManager(requireContext())
        rvReservas.adapter = adapter

        return view
    }

    override fun onResume() {
        super.onResume()
        // Volvemos a leer el usuario por si acaso al volver a la pestaña
        usuarioLogueado = requireActivity().intent.getParcelableExtra("USER_DATA")
        val userId = usuarioLogueado?.uid ?: 0L
        
        if (userId != 0L) {
            cargarMisReservas(userId)
        } else {
            tvEmptyState.text = "Error: No se ha identificado al usuario (ID es 0)."
            tvEmptyState.visibility = View.VISIBLE
            rvReservas.visibility = View.GONE
        }
    }

    private fun cargarMisReservas(userId: Long) {
        listaCombinada.clear() // Limpiamos para evitar duplicados al cambiar de pestaña repetidamente
        val api = RetrofitClient.getApiService()

        // 1. Pedimos las que ofreces (Vendedor)
        api.obtenerMisOfrecidas(userId).enqueue(object : Callback<List<Intercambio>> {
            override fun onResponse(call: Call<List<Intercambio>>, response: Response<List<Intercambio>>) {
                if (response.isSuccessful) {
                    response.body()?.let { listaCombinada.addAll(it) }
                }

                // 2. Pedimos las que has reservado (Comprador)
                api.obtenerMisReservas(userId).enqueue(object : Callback<List<Intercambio>> {
                    override fun onResponse(call2: Call<List<Intercambio>>, response2: Response<List<Intercambio>>) {
                        if (response2.isSuccessful) {
                            response2.body()?.let { listaCombinada.addAll(it) }
                        }
                        actualizarVista()
                    }

                    override fun onFailure(call2: Call<List<Intercambio>>, t: Throwable) {
                        Log.e("RESERVAS", "Error al cargar compras: ${t.message}")
                        actualizarVista()
                    }
                })
            }

            override fun onFailure(call: Call<List<Intercambio>>, t: Throwable) {
                Log.e("RESERVAS", "Error al cargar ventas: ${t.message}")
                Toast.makeText(requireContext(), "Error de red al cargar historial", Toast.LENGTH_SHORT).show()
                actualizarVista()
            }
        })
    }

    private fun actualizarVista() {
        // Ordenamos para que las más nuevas (ID más alto) salgan arriba
        listaCombinada.sortByDescending { it.id }

        adapter.actualizarDatos(listaCombinada)

        if (listaCombinada.isEmpty()) {
            tvEmptyState.text = "Aquí aparecerán tus intercambios y reservas activas."
            tvEmptyState.visibility = View.VISIBLE
            rvReservas.visibility = View.GONE
        } else {
            tvEmptyState.visibility = View.GONE
            rvReservas.visibility = View.VISIBLE
        }
    }
}