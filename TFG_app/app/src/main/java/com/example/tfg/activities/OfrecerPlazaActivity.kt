package com.example.tfg.activities

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.tfg.R
import com.example.tfg.databinding.ActivityOfrecerPlazaBinding
import com.example.tfg.models.Coche
import com.example.tfg.models.Intercambio
import com.example.tfg.network.RetrofitClient
import com.example.tfg.utils.SessionManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class OfrecerPlazaActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityOfrecerPlazaBinding
    private lateinit var mMap: GoogleMap
    
    private var isFaseFormulario = false
    private var selectedLocation: LatLng? = null
    private var listaCoches = mutableListOf<Coche>()
    private var cocheSeleccionadoId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOfrecerPlazaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configuración fluida de retroceso
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isFaseFormulario) {
                    volverFaseMapa()
                } else {
                    finish() // Sale de la app si está en el mapa
                }
            }
        })

        // Preparar Google Maps
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Eventos Visibles
        binding.btnConfirmLocation.setOnClickListener {
            if (::mMap.isInitialized) {
                selectedLocation = mMap.cameraPosition.target
                avanzarFaseFormulario()
            }
        }

        binding.etHoraSalida.setOnClickListener {
            mostrarTimePicker()
        }

        binding.btnPublishExchange.setOnClickListener {
            publicarPlaza()
        }

        val opcionesCapacidad = listOf("Moto-Bici", "Coche pequeño", "Coche mediano", "Coche grande", "Furgoneta", "Vehículo muy grande")
        val adapterCapacidad = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, opcionesCapacidad)
        binding.spinnerCapacidad.setAdapter(adapterCapacidad)

        cargarCochesUsuario()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // Coordenadas base arbitrarias o de GPS idealmente.
        val latLngInicio = LatLng(40.4168, -3.7038)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngInicio, 15f))
    }

    private fun cargarCochesUsuario() {
        val usuario = SessionManager.getUsuarioLogueado(this)
        if (usuario == null || usuario.uid == null) {
            Toast.makeText(this, "Falta inicio de sesión", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val call = RetrofitClient.getApiService().obtenerMisCoches(usuario.uid)
                val response = call.execute()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        listaCoches.clear()
                        listaCoches.addAll(response.body()!!)
                        
                        if (listaCoches.isEmpty()) {
                            Toast.makeText(this@OfrecerPlazaActivity, "Registra al menos un coche en tu cuenta.", Toast.LENGTH_LONG).show()
                        } else {
                            configurarSpinnerCoches()
                        }
                    } else {
                        Toast.makeText(this@OfrecerPlazaActivity, "Error conectando vehiculos", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@OfrecerPlazaActivity, "Fallo conectividad: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun configurarSpinnerCoches() {
        val stringsCoches = listaCoches.map { "${it.marca} ${it.modelo} (${it.matricula})" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, stringsCoches)
        binding.spinnerCars.setAdapter(adapter)

        binding.spinnerCars.setOnItemClickListener { _, _, position, _ ->
            cocheSeleccionadoId = listaCoches[position].cid
        }
    }

    private fun mostrarTimePicker() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            val horaFormateada = String.format("%02d:%02d", selectedHour, selectedMinute)
            binding.etHoraSalida.setText(horaFormateada)
        }, hour, minute, true)

        timePickerDialog.show()
    }

    private fun avanzarFaseFormulario() {
        isFaseFormulario = true
        binding.layoutPhaseMap.visibility = View.GONE
        binding.layoutPhaseForm.visibility = View.VISIBLE
    }

    private fun volverFaseMapa() {
        isFaseFormulario = false
        binding.layoutPhaseForm.visibility = View.GONE
        binding.layoutPhaseMap.visibility = View.VISIBLE
    }

    private fun publicarPlaza() {
        if (cocheSeleccionadoId == null || selectedLocation == null) {
            Toast.makeText(this, "Debe seleccionar la ubicación y el coche.", Toast.LENGTH_SHORT).show()
            return
        }

        val precioText = binding.etPrecio.text.toString()
        val capacidadText = binding.spinnerCapacidad.text.toString()
        val direccionText = binding.etDireccion.text.toString()
        val minutosText = binding.etMinutosCortesia.text.toString()
        val horaSalidaText = binding.etHoraSalida.text.toString()

        if (precioText.isEmpty() || horaSalidaText.isEmpty() || minutosText.isEmpty()) {
            Toast.makeText(this, "Debe rellenar todos los campos clave.", Toast.LENGTH_SHORT).show()
            return
        }

        val usuario = SessionManager.getUsuarioLogueado(this)
        
        // Formateo de fecha al estándar ISO 8601 esperado por LocalDateTime
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val horaISO = String.format("%04d-%02d-%02d %s:00", year, month, day, horaSalidaText)
        
        // Empaquetado DTO de Inmersión (El back recalcula precio a fondo)
        val nuevoIntercambio = Intercambio(
            idVendedor = usuario?.uid ?: return,
            idCocheVendedor = cocheSeleccionadoId!!,
            precioTotalComprador = precioText.toDoubleOrNull() ?: 0.0,
            comisionServicio = 0.0,
            gananciaVendedor = precioText.toDoubleOrNull() ?: 0.0,
            momentoIntercambioPrevisto = horaISO,
            cortesiaMinutos = minutosText.toIntOrNull() ?: 15,
            plazaLat = selectedLocation!!.latitude,
            plazaLong = selectedLocation!!.longitude,
            plazaDireccionTexto = direccionText.takeIf { it.isNotBlank() } ?: "Coordenadas fijadas",
            capacidad = capacidadText.takeIf { it.isNotBlank() },
            estadoIntercambio = "Esperando"
        )

        binding.progressBar.visibility = View.VISIBLE
        binding.btnPublishExchange.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val call = RetrofitClient.getApiService().ofrecerPlaza(nuevoIntercambio)
                val response = call.execute()

                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    if (response.isSuccessful) {
                        Toast.makeText(this@OfrecerPlazaActivity, "¡Tu Oferta ha volado exitosamente al Mapa!", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        binding.btnPublishExchange.isEnabled = true
                        val error = response.errorBody()?.string() ?: ""
                        Toast.makeText(this@OfrecerPlazaActivity, "Error de sistema: $error", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnPublishExchange.isEnabled = true
                    Toast.makeText(this@OfrecerPlazaActivity, "Conexión Extraviada: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}