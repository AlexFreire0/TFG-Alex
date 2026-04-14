package com.example.tfg.activities

import android.content.res.Configuration
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.tfg.R
import com.example.tfg.databinding.ActivityDetalleReservaBinding
import com.example.tfg.models.Intercambio
import com.example.tfg.network.RetrofitClient
import com.example.tfg.utils.SessionManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class DetalleReservaActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityDetalleReservaBinding
    private var idIntercambio: Long = -1L
    private var mMap: GoogleMap? = null
    private var plazaLocation: LatLng? = null
    private var currentUserId: Long = -1L  // ID del usuario en sesión

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetalleReservaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializamos el ciclo vital del MapView nativo 
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)

        // Obtener UID del usuario en sesión (para detectar si es el vendedor)
        currentUserId = SessionManager.getUsuarioLogueado(this)?.uid ?: -1L

        val idExtraStr = intent.getStringExtra("id_intercambio")
        idIntercambio = idExtraStr?.toLongOrNull() ?: -1L

        if (idIntercambio == -1L) {
            Toast.makeText(this, "Sesión de seguimiento perdida", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // TextWatcher: habilita el botón solo cuando el PIN tiene exactamente 4 dígitos
        binding.etPin.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.btnConfirmar.isEnabled = (s?.length ?: 0) == 4
            }
        })
        binding.btnConfirmar.isEnabled = false  // Deshabilitado hasta que haya 4 dígitos

        binding.btnConfirmar.setOnClickListener {
            completarReserva()
        }

        cargarDatos()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Mapa Oscuro si el sistema está en Dark Mode
        val isNightMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        if (isNightMode) {
            googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_night_style))
        }

        // Carga diferida: Si la API resolvió antes de que el mapa pintara, centramos ahora
        plazaLocation?.let {
            mMap?.addMarker(MarkerOptions().position(it).title("Ubicación de la Plaza"))
            mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 17f))
        }
    }

    private fun cargarDatos() {
        binding.loadingOverlay.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val call = RetrofitClient.getApiService().getIntercambioById(idIntercambio)
                val response = call.execute()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        updateUI(response.body()!!)
                    } else {
                        binding.loadingOverlay.visibility = View.GONE
                        Toast.makeText(this@DetalleReservaActivity, "Error al refrescar estado", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.loadingOverlay.visibility = View.GONE
                    Toast.makeText(this@DetalleReservaActivity, "Conexión Inestable", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateUI(reserva: Intercambio) {
        binding.tvDireccionCentral.text = reserva.plazaDireccionTexto ?: "Sector Geográfico Cifrado"
        binding.tvHoraSalidaMaster.text = "Salida Prevista: ${reserva.momentoIntercambioPrevisto?.replace("T", " ") ?: "--:--"}"

        val lat = reserva.plazaLat
        val lon = reserva.plazaLong
        if (lat != null && lon != null) {
             plazaLocation = LatLng(lat, lon)
             mMap?.let { map ->
                 map.clear()
                 map.addMarker(MarkerOptions().position(plazaLocation!!).title("Punto de Transferencia"))
                 map.animateCamera(CameraUpdateFactory.newLatLngZoom(plazaLocation!!, 17f))
             }
        }

        // Guardia de Expiración (cliente-side + estado del servidor)
        val estaExpirada = esExpiradaLocalmente(reserva) ||
                           reserva.estadoIntercambio?.equals("EXPIRADA", ignoreCase = true) == true
        if (estaExpirada) {
            binding.layoutWaitingBuyer.visibility = View.GONE
            binding.layoutBuyerAssigned.visibility = View.GONE
            binding.btnConfirmar.isEnabled = false
            binding.btnConfirmar.text = "OFERTA EXPIRADA"
            binding.tilPin.visibility = View.GONE
            binding.loadingOverlay.visibility = View.GONE
            binding.tvDireccionCentral.text = "⏱ Esta oferta ya no está activa"
            return
        }

        // Estado Completado
        val estaCompletado = reserva.estadoIntercambio?.equals("Completado", ignoreCase = true) == true ||
                             reserva.estadoIntercambio?.equals("FINALIZADO", ignoreCase = true) == true
        if (estaCompletado) {
            binding.btnConfirmar.isEnabled = false
            binding.tilPin.visibility = View.GONE
            binding.btnConfirmar.text = "MANDATO DE ENTREGA CERRADO"
        }

        // Estado P2P: Esperando vs Asignado
        val esperandoP2P = reserva.idComprador == null || reserva.estadoIntercambio?.equals("Esperando", ignoreCase = true) == true
        val estaReservado = reserva.estadoIntercambio?.equals("Reservado", ignoreCase = true) == true
        val esVendedor = reserva.idVendedor == currentUserId

        if (esperandoP2P) {
            binding.layoutWaitingBuyer.visibility = View.VISIBLE
            binding.layoutBuyerAssigned.visibility = View.GONE
            binding.tilPin.visibility = View.GONE        // El comprador aún no llegó, no hace falta PIN
            binding.btnConfirmar.visibility = View.GONE  // No hay nada que confirmar aún
            binding.loadingOverlay.visibility = View.GONE
        } else if (estaReservado && esVendedor) {
            // ✅ FLUJO DE CIERRE: El comprador está aquí y soy el dueño de la plaza
            binding.layoutWaitingBuyer.visibility = View.GONE
            binding.layoutBuyerAssigned.visibility = View.VISIBLE
            binding.tilPin.visibility = View.VISIBLE
            binding.btnConfirmar.visibility = View.VISIBLE
            binding.btnConfirmar.text = "Finalizar y Cobrar 💸"
            binding.btnConfirmar.isEnabled = false  // TextWatcher lo activa a los 4 dígitos
            if (reserva.idCocheComprador != null) {
                cargarCocheComprador(reserva.idCocheComprador)
            } else {
                binding.loadingOverlay.visibility = View.GONE
            }
        } else {
            // Comprador viendo una reserva activa suya — solo info, sin PIN
            binding.layoutWaitingBuyer.visibility = View.GONE
            binding.layoutBuyerAssigned.visibility = View.VISIBLE
            binding.tilPin.visibility = View.GONE
            binding.btnConfirmar.visibility = View.GONE
            if (reserva.idCocheComprador != null) {
                cargarCocheComprador(reserva.idCocheComprador)
            } else {
                binding.loadingOverlay.visibility = View.GONE
            }
        }
    }

    /**
     * Comprueba localmente si el tiempo del intercambio + cortesía ya ha expirado.
     * Solo aplica para reservas en estado "Esperando" (sin pago confirmado).
     */
    private fun esExpiradaLocalmente(reserva: Intercambio): Boolean {
        if (reserva.estadoIntercambio?.equals("Esperando", ignoreCase = true) != true) return false
        return try {
            val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val fecha = fmt.parse(reserva.momentoIntercambioPrevisto ?: return false) ?: return false
            val cortesia = (reserva.cortesiaMinutos.toLong() * 60 * 1000)
            System.currentTimeMillis() > (fecha.time + cortesia)
        } catch (e: Exception) { false }
    }

    private fun cargarCocheComprador(idCocheComprador: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val call = RetrofitClient.getApiService().obtenerDetalleCoche(idCocheComprador)
                val response = call.execute()

                withContext(Dispatchers.Main) {
                    binding.loadingOverlay.visibility = View.GONE
                    if (response.isSuccessful && response.body() != null) {
                        val vehiculo = response.body()!!
                        binding.tvModeloCoche.text = "${vehiculo.marca} ${vehiculo.modelo}"
                        binding.tvMatriculaComprador.text = vehiculo.matricula?.uppercase() ?: "RESTRINGIDA"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.loadingOverlay.visibility = View.GONE
                    binding.tvModeloCoche.text = "Vehículo: Conexión Aislada"
                }
            }
        }
    }

    private fun completarReserva() {
        val pin = binding.etPin.text.toString()
        if (pin.length < 4) {
            binding.tilPin.error = "Introduce el PIN de 4 dígitos"
            return
        }
        binding.tilPin.error = null
        binding.loadingOverlay.visibility = View.VISIBLE
        binding.btnConfirmar.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val call = RetrofitClient.getApiService().completarIntercambio(idIntercambio, pin)
                val response = call.execute()

                withContext(Dispatchers.Main) {
                    binding.loadingOverlay.visibility = View.GONE
                    if (response.isSuccessful) {
                        // 🎉 Celebración de Cierre Exitoso
                        Toast.makeText(
                            this@DetalleReservaActivity,
                            "🎉 ¡Intercambio Completado! El cobro está en camino.",
                            Toast.LENGTH_LONG
                        ).show()
                        // Congelar toda la UI como trofeo visual del éxito
                        binding.btnConfirmar.text = "INTERCAMBIO CERRADO ✅"
                        binding.btnConfirmar.isEnabled = false
                        binding.tilPin.visibility = View.GONE
                    } else if (response.code() == 400 || response.code() == 403) {
                        // PIN incorrecto — error orientado al vendedor
                        binding.tilPin.error = "PIN incorrecto. Pídelo al comprador."
                        binding.btnConfirmar.isEnabled = false  // TextWatcher volverá a activarlo al cambiar el PIN
                    } else {
                        binding.btnConfirmar.isEnabled = true
                        Toast.makeText(
                            this@DetalleReservaActivity,
                            "Error del servidor (${response.code()}). Vuelve a intentarlo.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.loadingOverlay.visibility = View.GONE
                    binding.btnConfirmar.isEnabled = true
                    Toast.makeText(this@DetalleReservaActivity, "Sin conexión. Comprueba tu red.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // =======================================================
    // SUBSISTEMA NATIVO DE MAPS API - DELEGACIÓN DE LIFECYCLE
    // =======================================================

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }
}
