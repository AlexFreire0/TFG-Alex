package com.example.tfg.activities

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.tfg.R
import com.example.tfg.databinding.ActivityOfrecerPlazaBinding
import com.example.tfg.models.Coche
import com.example.tfg.models.Intercambio
import com.example.tfg.models.Usuario
import com.example.tfg.network.RetrofitClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class OfrecerPlazaActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityOfrecerPlazaBinding
    private lateinit var mMap: GoogleMap
    private var markerSeleccionado: Marker? = null
    private var ubicacionFinal: LatLng? = null

    private var usuarioLogueado: Usuario? = null
    private var listaCoches = mutableListOf<Coche>()
    private var horaSeleccionadaFormateada: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOfrecerPlazaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        usuarioLogueado = intent.getParcelableExtra("USER_DATA")

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_ofrecer) as SupportMapFragment
        mapFragment.getMapAsync(this)

        cargarCochesVendedor()

        // Cálculo de ganancias en tiempo real
        binding.etPrecio.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val precioTotal = s.toString().toDoubleOrNull() ?: 0.0
                val comision = precioTotal * 0.15
                val netoVendedor = precioTotal - comision
                binding.tvGananciaVendedor.text = "Tú recibirás: ${"%.2f".format(netoVendedor)}€"
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Selector de hora
        binding.btnSeleccionarHora.setOnClickListener {
            mostrarSelectorHora()
        }

        binding.btnPublicar.setOnClickListener {
            publicarMiPlaza()
        }
    }

    private fun mostrarSelectorHora() {
        val cal = Calendar.getInstance()
        val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, 0)

            // Formato para el Backend (yyyy-MM-dd HH:mm:ss)
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            horaSeleccionadaFormateada = sdf.format(cal.time)

            binding.tvHoraSeleccionada.text = "Saldré a las: ${"%02d:%02d".format(hour, minute)}"
        }

        TimePickerDialog(this, timeSetListener,
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE), true).show()
    }

    private fun cargarCochesVendedor() {
        val idUsuario = usuarioLogueado?.uid ?: return
        RetrofitClient.getApiService().obtenerMisCoches(idUsuario).enqueue(object : Callback<List<Coche>> {
            override fun onResponse(call: Call<List<Coche>>, response: Response<List<Coche>>) {
                if (response.isSuccessful && response.body() != null) {
                    listaCoches = response.body()!!.toMutableList()
                    if (listaCoches.isEmpty()) {
                        Toast.makeText(this@OfrecerPlazaActivity, "Registra un coche primero", Toast.LENGTH_LONG).show()
                        finish()
                        return
                    }
                    val nombresCoches = listaCoches.map { "${it.marca} ${it.modelo} (${it.matricula})" }
                    val adapter = ArrayAdapter(this@OfrecerPlazaActivity, android.R.layout.simple_spinner_dropdown_item, nombresCoches)
                    binding.spinnerCoches.adapter = adapter
                }
            }
            override fun onFailure(call: Call<List<Coche>>, t: Throwable) {
                Toast.makeText(this@OfrecerPlazaActivity, "Error de red", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
            LocationServices.getFusedLocationProviderClient(this).lastLocation.addOnSuccessListener { loc ->
                val pos = if (loc != null) LatLng(loc.latitude, loc.longitude) else LatLng(40.4167, -3.7033)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 16f))
                actualizarMarcador(pos)
            }
        }
        mMap.setOnMapClickListener { latLng -> actualizarMarcador(latLng) }
    }

    private fun actualizarMarcador(pos: LatLng) {
        markerSeleccionado?.remove()
        ubicacionFinal = pos
        markerSeleccionado = mMap.addMarker(MarkerOptions()
            .position(pos)
            .title("Mi ubicación")
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)))
    }

    private fun publicarMiPlaza() {
        val precioTotal = binding.etPrecio.text.toString().toDoubleOrNull() ?: 0.0
        val cochePos = binding.spinnerCoches.selectedItemPosition
        val idUsuario = usuarioLogueado?.uid
        val minutosCortesia = binding.etCortesia.text.toString().toIntOrNull() ?: 5

        if (idUsuario == null || ubicacionFinal == null || precioTotal <= 0 || cochePos == -1 || horaSeleccionadaFormateada == null) {
            Toast.makeText(this, "Completa todos los campos y la hora", Toast.LENGTH_SHORT).show()
            return
        }

        val coche = listaCoches[cochePos]
        val comision = precioTotal * 0.15
        val gananciaNeto = precioTotal - comision

        val intercambio = Intercambio(
            id = null,
            idVendedor = idUsuario,
            idCocheVendedor = coche.cid!!,
            precioTotalComprador = precioTotal,
            comisionServicio = comision,
            gananciaVendedor = gananciaNeto,
            momentoIntercambioPrevisto = horaSeleccionadaFormateada!!,
            cortesiaMinutos = minutosCortesia,
            plazaLat = ubicacionFinal!!.latitude,
            plazaLong = ubicacionFinal!!.longitude,
            estadoIntercambio = "Esperando"
        )

        RetrofitClient.getApiService().ofrecerPlaza(intercambio).enqueue(object : Callback<Intercambio> {
            override fun onResponse(call: Call<Intercambio>, response: Response<Intercambio>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@OfrecerPlazaActivity, "¡Plaza publicada!", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    val error = response.errorBody()?.string()
                    Log.e("API_ERROR", error ?: "Unknown")
                    Toast.makeText(this@OfrecerPlazaActivity, "Error: $error", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<Intercambio>, t: Throwable) {
                Toast.makeText(this@OfrecerPlazaActivity, "Sin conexión", Toast.LENGTH_SHORT).show()
            }
        })
    }
}