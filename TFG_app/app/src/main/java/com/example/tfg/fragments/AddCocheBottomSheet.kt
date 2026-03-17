package com.example.tfg.fragments

import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.tfg.databinding.FragmentAddCocheBottomSheetBinding
import com.example.tfg.models.Coche
import com.example.tfg.models.Usuario
import com.example.tfg.network.RetrofitClient
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AddCocheBottomSheet(
    private val onCocheAdded: (Coche) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: FragmentAddCocheBottomSheetBinding? = null
    private val binding get() = _binding!!

    // Mapa simulado para almacenar IDs reales de la API
    // En real seria algo como List<MarcaApiItem>
    private val marcasDisponibles = mutableMapOf<String, Int>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddCocheBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Forzar Matrícula a mayúsculas
        binding.etMatricula.filters = arrayOf(InputFilter.AllCaps())

        // 1. Cargar Marcas al abrir el diálogo
        cargarMarcasRetrofit()

        // 2. Escuchar selección de Marca para cargar Modelos (API)
        binding.etMarcaDropdown.setOnItemClickListener { parent, _, position, _ ->
            val marcaSeleccionada = parent.getItemAtPosition(position).toString()
            val idMarca = marcasDisponibles[marcaSeleccionada]

            binding.etModeloDropdown.text.clear()
            binding.etModeloDropdown.isEnabled = false
            binding.layoutModelo.isEnabled = false

            if (idMarca != null) {
                cargarModelosRetrofit(idMarca)
            }
        }

        // 2.1. Escuchar escritura manual libre
        binding.etMarcaDropdown.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val texto = s?.toString()?.trim() ?: ""
                // Si el usuario escribe algo (esté o no en la API), habilitamos el campo Modelo
                if (texto.isNotEmpty()) {
                    binding.layoutModelo.isEnabled = true
                    binding.etModeloDropdown.isEnabled = true
                } else {
                    binding.layoutModelo.isEnabled = false
                    binding.etModeloDropdown.isEnabled = false
                    binding.etModeloDropdown.text.clear()
                }
            }
        })

        // 3. Listener del Botón Guardar
        binding.btnGuardarCoche.setOnClickListener {
            val marca = binding.etMarcaDropdown.text.toString().trim()
            val modelo = binding.etModeloDropdown.text.toString().trim()
            val matricula = binding.etMatricula.text.toString().trim()

            if (validarCampos(marca, modelo, matricula)) {
                val idReal = com.example.tfg.utils.SessionManager.getUsuarioId(requireContext())
                if (idReal == -1L) {
                    Toast.makeText(requireContext(), "Error de sesión", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Instanciar Coche usando el ID real del SessionManager
                val currentUser = Usuario(uid = idReal, nombre = "", correo = "")
                val cocheNuevo = Coche(null, currentUser, marca, modelo, matricula)
                
                guardarCocheRetrofit(cocheNuevo)
            }
        }
    }

    private fun validarCampos(marca: String, modelo: String, matricula: String): Boolean {
        var isValid = true

        // NOTA: Como ahora usamos Dropdowns, en lugar de error en TextInput, 
        // puedes usar error en el InputLayout si no han seleccionado nada
        if (marca.isEmpty()) {
            binding.etMarcaDropdown.error = "Selecciona una marca"
            isValid = false
        } else {
            binding.etMarcaDropdown.error = null
        }

        if (modelo.isEmpty()) {
            binding.etModeloDropdown.error = "Selecciona un modelo"
            isValid = false
        } else {
            binding.etModeloDropdown.error = null
        }

        if (matricula.isEmpty()) {
            binding.etMatricula.error = "Ingresa la matrícula"
            isValid = false
        } else {
            binding.etMatricula.error = null
        }

        return isValid
    }

    // =======================================================
    // ÁMBITO RETROFIT / CORRUTINAS (Reemplazar con tu ApiService)
    // =======================================================

    private fun cargarMarcasRetrofit() {
        mostrarLoading(true)
        
        // --- SUSTITUIR POR: lifecycleScope.launch { val response = apiService.obtenerMarcas() ... } ---
        lifecycleScope.launch(Dispatchers.IO) {
            delay(800) // Simulando latencia de red
            
            val marcasApi = mapOf("Toyota" to 101, "Ford" to 102, "Seat" to 103, "Audi" to 104)
            
            withContext(Dispatchers.Main) {
                marcasDisponibles.clear()
                marcasDisponibles.putAll(marcasApi)

                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, marcasApi.keys.toList())
                binding.etMarcaDropdown.setAdapter(adapter)
                
                mostrarLoading(false)
            }
        }
    }

    private fun cargarModelosRetrofit(idMarca: Int) {
        mostrarLoading(true)

        // --- SUSTITUIR POR: lifecycleScope.launch { val response = apiService.obtenerModelos(idMarca) ... } ---
        lifecycleScope.launch(Dispatchers.IO) {
            delay(600) // Simulando red
            
            // Modelos Fake basados en ID de marca
            val modelosApi = when(idMarca) {
                101 -> listOf("Corolla", "Yaris", "RAV4")
                102 -> listOf("Focus", "Fiesta", "Mustang")
                103 -> listOf("Ibiza", "Leon", "Ateca")
                104 -> listOf("A3", "A4", "Q5")
                else -> listOf("Modelo Desconocido")
            }

            withContext(Dispatchers.Main) {
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, modelosApi)
                
                binding.etModeloDropdown.setAdapter(adapter)
                // Desbloqueamos el input de Modelo ahora que tenemos datos
                binding.layoutModelo.isEnabled = true
                binding.etModeloDropdown.isEnabled = true
                
                mostrarLoading(false)
            }
        }
    }

    private fun guardarCocheRetrofit(cochePayload: Coche) {
        mostrarLoading(true)
        binding.btnGuardarCoche.isEnabled = false
        binding.btnGuardarCoche.text = "Guardando..."

        val apiService = RetrofitClient.getApiService()

        // Hacemos la llamada REAL a tu Spring Boot
        apiService.registrarCoche(cochePayload).enqueue(object : Callback<Coche> {
            override fun onResponse(call: Call<Coche>, response: Response<Coche>) {
                mostrarLoading(false)
                binding.btnGuardarCoche.isEnabled = true
                binding.btnGuardarCoche.text = "Guardar Coche"

                if (response.isSuccessful) {
                    val cocheGuardado = response.body()
                    if (cocheGuardado != null) {
                        // Ahora sí, le pasamos al Fragment el coche REAL que ha devuelto la base de datos
                        onCocheAdded(cocheGuardado)
                        dismiss()
                    }
                } else {
                    // Si Spring Boot lo rechaza (ej. error 400), lo mostramos
                    Toast.makeText(requireContext(), "Error del servidor: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<Coche>, t: Throwable) {
                mostrarLoading(false)
                binding.btnGuardarCoche.isEnabled = true
                binding.btnGuardarCoche.text = "Guardar Coche"
                Toast.makeText(requireContext(), "Fallo de conexión: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun mostrarLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
