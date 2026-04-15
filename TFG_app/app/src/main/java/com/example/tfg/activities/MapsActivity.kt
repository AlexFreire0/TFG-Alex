package com.example.tfg.activities

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.tfg.R
import com.example.tfg.databinding.ActivityMapsBinding
import com.example.tfg.models.Coche
import com.example.tfg.models.Intercambio
import com.example.tfg.models.Usuario
import com.example.tfg.models.PagoResponse
import com.example.tfg.models.PagoRequest
import com.example.tfg.network.RetrofitClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private val radioBusqueda = 5000.0
    private var usuarioLogueado: Usuario? = null

    // --- VARIABLES PARA STRIPE ---
    private lateinit var paymentSheet: PaymentSheet
    private var pendingIntercambioId: Long? = null
    private var pendingIdCocheComprador: Long? = null
    private var pendingDialog: BottomSheetDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Recuperamos el usuario
        usuarioLogueado = com.example.tfg.utils.SessionManager.getUsuarioLogueado(this)

        // --- INICIALIZAR STRIPE ---
        // ¡Sustituye esto por tu clave pk_test_...!
        PaymentConfiguration.init(this, "pk_test_51T5PRy6QQuaydOoxuqiE9T5xdq2QwVHjGUDK85VQrLd0lLDeOOyUfj5LGw8egXD2BRzM7X1tuKWjHyncwokk1CPS00Jg5vrZwd")
        paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.btnBack.setOnClickListener { finish() }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true

        mMap.setOnMarkerClickListener { marker ->
            android.util.Log.d("MapsActivity", "Clic detectado en el marcador")
            val intercambio = marker.tag as? Intercambio
            android.util.Log.d("MapsActivity", "Valor del tag intercambio = $intercambio")
            if (intercambio != null) {
                mostrarDialogoReserva(intercambio)
            }
            true
        }

        if (checkLocationPermission()) {
            setupMapaConUbicacion()
        }
    }

    private fun mostrarDialogoReserva(plaza: Intercambio) {
        android.util.Log.d("MapsActivity", "Iniciando mostrarDialogoReserva. usuarioLogueado = $usuarioLogueado")
        val dialog = BottomSheetDialog(this)
        val vista = layoutInflater.inflate(R.layout.layout_confirmar_reserva, null)
        dialog.setContentView(vista)

        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.setBackgroundResource(android.R.color.transparent)

        vista.findViewById<TextView>(R.id.tvReservaPrecio).text = "${"%.2f".format(plaza.precioTotalComprador)}€"

        if (plaza.momentoIntercambioPrevisto.length >= 16) {
            vista.findViewById<TextView>(R.id.tvReservaHora).text = "Salida prevista: ${plaza.momentoIntercambioPrevisto.substring(11, 16)}"
        }

        val dropdownCoches = vista.findViewById<MaterialAutoCompleteTextView>(R.id.dropdownCoches)
        val btnConfirmar = vista.findViewById<Button>(R.id.btnConfirmarReserva)

        btnConfirmar.isEnabled = false
        dropdownCoches.setText("Cargando vehículos...", false)

        var idCocheSeleccionado: Long? = null
        val userId = usuarioLogueado?.uid
        android.util.Log.d("MapsActivity", "El usuario detectado es válido: ID = $userId")
        if (userId == null) {
            Toast.makeText(this, "Error: Sesión no válida", Toast.LENGTH_LONG).show()
            return
        }

        RetrofitClient.getApiService().obtenerMisCoches(userId).enqueue(object : Callback<List<Coche>> {
            override fun onResponse(call: Call<List<Coche>>, response: Response<List<Coche>>) {
                if (response.isSuccessful) {
                    val listaCoches = response.body() ?: emptyList()

                    if (listaCoches.isEmpty()) {
                        dropdownCoches.setText("No tienes vehículos registrados", false)
                        Toast.makeText(this@MapsActivity, "Debes registrar un coche primero", Toast.LENGTH_LONG).show()
                    } else {
                        dropdownCoches.setText("", false)

                        val nombresCoches = listaCoches.map { "${it.marca} ${it.modelo} (${it.matricula})" }

                        val adapterCoches = ArrayAdapter(this@MapsActivity, android.R.layout.simple_dropdown_item_1line, nombresCoches)
                        dropdownCoches.setAdapter(adapterCoches)

                        dropdownCoches.setOnItemClickListener { _, _, position, _ ->
                            idCocheSeleccionado = listaCoches[position].cid
                            btnConfirmar.isEnabled = true
                        }

                        // --- AQUÍ CAMBIA LA LÓGICA (INTERCEPTAMOS EL PAGO) ---
                        btnConfirmar.setOnClickListener {
                            if (idCocheSeleccionado != null) {
                                btnConfirmar.isEnabled = false
                                btnConfirmar.text = "PROCESANDO PAGO..."

                                // Guardamos los datos temporalmente
                                pendingIntercambioId = plaza.id
                                pendingIdCocheComprador = idCocheSeleccionado
                                pendingDialog = dialog

                                // Llamamos a nuestro servidor para preparar Stripe
                                iniciarPagoStripe(plaza, btnConfirmar)
                            }
                        }
                    }
                }
            }

            override fun onFailure(call: Call<List<Coche>>, t: Throwable) {
                dropdownCoches.setText("Error al cargar vehículos", false)
                Toast.makeText(this@MapsActivity, "Error de red al obtener coches", Toast.LENGTH_SHORT).show()
            }
        })

        dialog.show()
    }

    // --- NUEVO: PREPARAR EL PAGO CON SPRING BOOT ---
    private fun iniciarPagoStripe(plaza: Intercambio, btnConfirmar: Button) {
        val requestData = PagoRequest(
            idIntercambio = plaza.id ?: 0L,
            idUsuario = usuarioLogueado?.uid ?: 0L,
            idVendedor = plaza.idVendedor
        )

        RetrofitClient.getApiService().crearPaymentSheet(requestData).enqueue(object : Callback<PagoResponse> {
            override fun onResponse(call: Call<PagoResponse>, response: Response<PagoResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val pagoInfo = response.body()!!

                    // Configuramos la hoja de pago de Stripe
                    val customerConfig = PaymentSheet.CustomerConfiguration(
                        pagoInfo.customer,
                        pagoInfo.ephemeralKey
                    )

                    val paymentSheetConfig = PaymentSheet.Configuration(
                        merchantDisplayName = "ParkingHole",
                        customer = customerConfig,
                        allowsDelayedPaymentMethods = true
                    )

                    // ¡Abrimos la pasarela de pago real!
                    paymentSheet.presentWithPaymentIntent(pagoInfo.paymentIntent, paymentSheetConfig)

                    // Restauramos el botón por si el usuario cancela el pago y quiere volver a darle
                    btnConfirmar.isEnabled = true
                    btnConfirmar.text = "CONFIRMAR RESERVA"

                } else {
                    btnConfirmar.isEnabled = true
                    btnConfirmar.text = "CONFIRMAR RESERVA"
                    Toast.makeText(this@MapsActivity, "Error: El vendedor no admite pagos", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<PagoResponse>, t: Throwable) {
                btnConfirmar.isEnabled = true
                btnConfirmar.text = "CONFIRMAR RESERVA"
                Toast.makeText(this@MapsActivity, "Error de red al conectar con el banco", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // --- NUEVO: RESULTADO DEL PAGO ---
    private fun onPaymentSheetResult(paymentSheetResult: PaymentSheetResult) {
        when (paymentSheetResult) {
            is PaymentSheetResult.Canceled -> {
                Toast.makeText(this, "Pago cancelado", Toast.LENGTH_SHORT).show()
            }
            is PaymentSheetResult.Failed -> {
                Toast.makeText(this, "Error en el pago: ${paymentSheetResult.error.message}", Toast.LENGTH_LONG).show()
            }
            is PaymentSheetResult.Completed -> {
                // ¡EL PAGO SE HA COBRADO CON ÉXITO!
                Toast.makeText(this, "¡Pago completado!", Toast.LENGTH_SHORT).show()

                // Ahora sí, ejecutamos la reserva oficial en nuestra base de datos
                if (pendingIntercambioId != null && pendingIdCocheComprador != null && pendingDialog != null) {
                    ejecutarReserva(pendingIntercambioId!!, pendingIdCocheComprador!!, pendingDialog!!)
                }
            }
        }
    }

    private fun ejecutarReserva(intercambioId: Long, idCocheComprador: Long, dialog: BottomSheetDialog) {
        val idComprador = usuarioLogueado?.uid ?: return

        RetrofitClient.getApiService().reservarPlaza(intercambioId, idComprador, idCocheComprador).enqueue(object : Callback<Intercambio> {
            override fun onResponse(call: Call<Intercambio>, response: Response<Intercambio>) {
                if (response.isSuccessful) {
                    dialog.dismiss()
                    Toast.makeText(this@MapsActivity, "¡Plaza tuya! Revisa el Radar.", Toast.LENGTH_LONG).show()
                    setupMapaConUbicacion()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Error al reservar"
                    Toast.makeText(this@MapsActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Intercambio>, t: Throwable) {
                Toast.makeText(this@MapsActivity, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupMapaConUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 14f))
                    cargarIntercambiosEnMapa(it)
                } ?: cargarIntercambiosEnMapa(null)
            }
        }
    }

    private fun cargarIntercambiosEnMapa(miUbicacion: Location?) {
        val idUsuarioConsulta = usuarioLogueado?.uid
        RetrofitClient.getApiService().obtenerIntercambiosDisponibles(idUsuarioConsulta).enqueue(object : Callback<List<Intercambio>> {
            override fun onResponse(call: Call<List<Intercambio>>, response: Response<List<Intercambio>>) {
                if (response.isSuccessful) {
                    val lista = response.body() ?: emptyList()
                    mMap.clear()

                    for (intercambio in lista) {
                        val plazaLoc = Location("").apply {
                            latitude = intercambio.plazaLat
                            longitude = intercambio.plazaLong
                        }

                        val distancia = miUbicacion?.distanceTo(plazaLoc) ?: 0f

                        if (miUbicacion == null || distancia <= radioBusqueda) {
                            val posicion = LatLng(intercambio.plazaLat, intercambio.plazaLong)
                            val marker = mMap.addMarker(MarkerOptions()
                                .position(posicion)
                                .title("${intercambio.precioTotalComprador}€")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                            )
                            marker?.tag = intercambio
                        }
                    }
                }
            }
            override fun onFailure(call: Call<List<Intercambio>>, t: Throwable) {
                Toast.makeText(this@MapsActivity, "Error al cargar plazas", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun checkLocationPermission(): Boolean {
        return if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            false
        } else true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupMapaConUbicacion()
        }
    }
}