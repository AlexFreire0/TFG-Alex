package com.example.tfg.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tfg.adapters.CocheAdapter
import com.example.tfg.databinding.FragmentMisCochesBinding
import com.example.tfg.models.Coche
import com.example.tfg.network.RetrofitClient
import android.telecom.Call
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import okhttp3.ResponseBody
import retrofit2.Callback
import retrofit2.Response

class MisCochesFragment : Fragment() {

    private var _binding: FragmentMisCochesBinding? = null
    private val binding get() = _binding!!

    private lateinit var cocheAdapter: CocheAdapter
    private var listaCoches: MutableList<Coche> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMisCochesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurarRecyclerView()
        configurarFab()

        cargarCochesRetrofit()
    }

    private fun configurarRecyclerView() {
        cocheAdapter = CocheAdapter(listaCoches) { coche ->
            // Mostrar diálogo de confirmación nativo de Material Design
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Eliminar vehículo")
                .setMessage("¿Estás seguro de que deseas eliminar tu ${coche.marca} ${coche.modelo}?")
                .setNegativeButton("Cancelar") { dialog, _ ->
                    dialog.dismiss() // No hacer nada si se cancela
                }
                .setPositiveButton("Sí, eliminar") { _, _ ->
                    eliminarCoche(coche)
                }
                .show()
        }

        binding.rvCoches.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCoches.adapter = cocheAdapter
    }

    private fun eliminarCoche(coche: Coche) {
        val cid = coche.cid ?:0L
        if (cid == 0L) return
        // a) Mostrar el ProgressBar
        binding.pbLoadingCoches.visibility = View.VISIBLE
        
        val apiService = RetrofitClient.getApiService()
        
        // b) Hacer la llamada asíncrona a Retrofit
        apiService.eliminarCoche(cid).enqueue(object : retrofit2.Callback<ResponseBody> {
            override fun onResponse(
                call: retrofit2.Call<ResponseBody>,
                response: retrofit2.Response<ResponseBody>
            ) {
                // Hay que usar la ruta completa de retrofit2 si hay conflicto con android.telecom.Call
                if (response.isSuccessful) {
                    // c) En el onResponse exitoso: Mostrar Toast y recargar lista
                    Toast.makeText(requireContext(), "Vehículo eliminado correctamente", Toast.LENGTH_SHORT).show()
                    cargarCochesRetrofit()
                } else {
                    // d) En caso de error del servidor
                    binding.pbLoadingCoches.visibility = View.GONE
                    Log.e("API_COCHES", "Error al eliminar: ${response.code()}")
                    Toast.makeText(requireContext(), "Error al eliminar el vehículo", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: retrofit2.Call<ResponseBody>, t: Throwable) {
                // d) En caso de fallo de red
                binding.pbLoadingCoches.visibility = View.GONE
                Log.e("API_COCHES", "Fallo de red al eliminar: ${t.message}")
                Toast.makeText(requireContext(), "Error de red al intentar eliminar", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun configurarFab() {
        binding.fabAddCoche.setOnClickListener {
            val bottomSheet = AddCocheBottomSheet { nuevoCoche ->
                listaCoches.add(nuevoCoche)
                actualizarVista(listaCoches)
                Toast.makeText(requireContext(), "Vehículo insertado", Toast.LENGTH_SHORT).show()
            }
            bottomSheet.show(childFragmentManager, "AddCocheBottomSheet")
        }
    }

    private fun cargarCochesRetrofit() {
        binding.pbLoadingCoches.visibility = View.VISIBLE
        binding.rvCoches.visibility = View.GONE
        binding.llEmptyState.visibility = View.GONE

        val uidSimulado = 1L
        val apiService = RetrofitClient.getApiService()

        // Forzamos la ruta completa de retrofit2 para evitar el error de "Call"
        apiService.obtenerMisCoches(uidSimulado).enqueue(object : retrofit2.Callback<List<Coche>> {

            override fun onResponse(
                call: retrofit2.Call<List<Coche>>,
                response: retrofit2.Response<List<Coche>>
            ) {
                binding.pbLoadingCoches.visibility = View.GONE

                if (response.isSuccessful) {
                    val cochesRecibidos = response.body() ?: emptyList()

                    // LOG CRÍTICO: Mira esto en el Logcat
                    Log.d("API_COCHES", "JSON CRUDO: ${response.raw()}")
                    Log.d("API_COCHES", "Coches procesados: ${cochesRecibidos.size}")

                    listaCoches.clear()
                    listaCoches.addAll(cochesRecibidos)
                    actualizarVista(listaCoches)
                } else {
                    Log.e("API_COCHES", "Error del servidor: ${response.code()}")
                    actualizarVista(emptyList())
                }
            }

            override fun onFailure(call: retrofit2.Call<List<Coche>>, t: Throwable) {
                binding.pbLoadingCoches.visibility = View.GONE
                Log.e("API_COCHES", "Fallo de red: ${t.message}")
                actualizarVista(emptyList())
            }
        })
    }

    // Maneja Empty State interactivo, Límite de Coches y FAB
    fun actualizarVista(lista: List<Coche>) {
        // SIEMPRE actualizamos el adaptador, tanto si hay datos como si no
        cocheAdapter.actualizarCoches(lista)
        
        // Manejar Empty State y RecyclerView
        if (lista.isEmpty()) {
            binding.rvCoches.visibility = View.GONE
            binding.llEmptyState.visibility = View.VISIBLE
        } else {
            binding.rvCoches.visibility = View.VISIBLE
            binding.llEmptyState.visibility = View.GONE
        }

        // Manejar límite de 2 coches
        if (lista.size >= 2) {
            binding.fabAddCoche.visibility = View.GONE
            binding.cvLimitReached.visibility = View.VISIBLE
        } else {
            binding.fabAddCoche.visibility = View.VISIBLE
            binding.cvLimitReached.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}