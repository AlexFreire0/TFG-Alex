package com.example.tfg.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tfg.R
import com.example.tfg.activities.DetalleReservaActivity
import com.example.tfg.adapters.ReservasAdapter
import com.example.tfg.models.Intercambio
import com.example.tfg.network.RetrofitClient
import com.example.tfg.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReservasFragment : Fragment() {

    private lateinit var rvReservas: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var adapter: ReservasAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_reservas, container, false)
        rvReservas = view.findViewById(R.id.rvMisReservas)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val usuarioLogueado = SessionManager.getUsuarioLogueado(requireContext())
        val userId = usuarioLogueado?.uid

        if (userId == null || userId == 0L) {
            Log.e("MisReservasFragment", "Error: UID Nulo o Cero")
            Toast.makeText(requireContext(), "Sesión caducada", Toast.LENGTH_SHORT).show()
            actualizarVistaUI(emptyList())
            return
        }

        // Configuración inicial del Adaptador y Listener de Detalle
        adapter = ReservasAdapter(emptyList(), userId) { reserva ->
            val intent = Intent(requireContext(), DetalleReservaActivity::class.java)
            intent.putExtra("id_intercambio", reserva.id.toString())
            startActivity(intent)
        }

        rvReservas.layoutManager = LinearLayoutManager(requireContext())
        rvReservas.adapter = adapter

        cargarListaUnificada(userId)
    }

    private fun cargarListaUnificada(userId: Long) {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                // Descarga Paralela Usando Corrutinas
                val deferredReservas = async(Dispatchers.IO) { 
                    RetrofitClient.getApiService().obtenerMisReservas(userId).execute() 
                }
                val deferredOfrecidas = async(Dispatchers.IO) { 
                    RetrofitClient.getApiService().obtenerMisOfrecidas(userId).execute() 
                }

                // Esperamos la barrera de sincronización
                val responseReservas = deferredReservas.await()
                val responseOfrecidas = deferredOfrecidas.await()

                val listaReservas = if (responseReservas.isSuccessful) responseReservas.body() ?: emptyList() else emptyList()
                val listaOfrecidas = if (responseOfrecidas.isSuccessful) responseOfrecidas.body() ?: emptyList() else emptyList()

                // Fusión P2P Completa y Ordenación Cronológica por Fecha de Salida Prevista (Lo más reciente primero)
                val listaUnificada = (listaReservas + listaOfrecidas).sortedByDescending { it.momentoIntercambioPrevisto }

                actualizarVistaUI(listaUnificada)

                if (!responseReservas.isSuccessful || !responseOfrecidas.isSuccessful) {
                    Toast.makeText(requireContext(), "Alguna agenda de servidor falló al sincronizar.", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e("MisReservas", "Excepción de Red al fusionar agendas: ${e.message}")
                Toast.makeText(requireContext(), "Sin conexión para sincronizar Historial", Toast.LENGTH_SHORT).show()
                actualizarVistaUI(emptyList()) // Despejar y proteger
            }
        }
    }

    private fun actualizarVistaUI(listaUnificada: List<Intercambio>) {
        adapter.actualizarDatos(listaUnificada)

        if (listaUnificada.isEmpty()) {
            tvEmptyState.text = "Aún no tienes reservas activas"
            tvEmptyState.visibility = View.VISIBLE
            rvReservas.visibility = View.GONE
        } else {
            tvEmptyState.visibility = View.GONE
            rvReservas.visibility = View.VISIBLE
        }
    }
}