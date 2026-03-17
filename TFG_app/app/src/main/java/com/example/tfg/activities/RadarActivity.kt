package com.example.tfg.activities

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tfg.R
import com.example.tfg.models.Coche
import com.example.tfg.models.Intercambio
import com.example.tfg.models.Usuario
import com.example.tfg.network.RetrofitClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RadarActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var intercambio: Intercambio? = null
    private var usuarioLogueado: Usuario? = null

    // Elementos de la UI
    private lateinit var tvCocheInfo: TextView
    private lateinit var tvEstadoTitulo: TextView
    private lateinit var tvDistanciaInfo: TextView
    private lateinit var btnAccion: Button
    private lateinit var tvPinComprador: TextView
    private lateinit var etPinVendedor: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_radar)

        // 1. Recuperar datos del Intent
        intercambio = intent.getParcelableExtra("INTERCAMBIO_DATA")
        usuarioLogueado = intent.getParcelableExtra("USER_DATA")

        if (intercambio == null) {
            Toast.makeText(this, "Error al cargar el radar", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 2. Vincular vistas
        tvCocheInfo = findViewById(R.id.tvCocheInfo)
        tvEstadoTitulo = findViewById(R.id.tvEstadoTitulo)
        tvDistanciaInfo = findViewById(R.id.tvDistanciaInfo)
        btnAccion = findViewById(R.id.btnAccionPrincipal)
        tvPinComprador = findViewById(R.id.tvPinComprador)
        etPinVendedor = findViewById(R.id.etPinVendedor)

        // 3. Configurar UI según el rol (Vendedor vs Comprador)
        configurarInterfazPorRol()

        // 4. Inicializar Mapa
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapRadar) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // 5. Botón Volver
        findViewById<View>(R.id.btnBackRadar).setOnClickListener { finish() }

        // 6. Cargar detalles del vehículo
        cargarDatosVehiculo()
    }

    private fun configurarInterfazPorRol() {
        val esVendedor = usuarioLogueado?.uid == intercambio?.idVendedor

        if (esVendedor) {
            tvEstadoTitulo.text = "Esperando al comprador..."
            tvDistanciaInfo.text = "Pídele el PIN cuando llegue"

            // Mostramos la caja de texto al vendedor
            etPinVendedor.visibility = View.VISIBLE
            tvPinComprador.visibility = View.GONE

            btnAccion.text = "VALIDAR PIN Y COBRAR"

            btnAccion.setOnClickListener {
                val pinIntroducido = etPinVendedor.text.toString()
                if (pinIntroducido.length == 4) {
                    completarIntercambioConPin(pinIntroducido)
                } else {
                    Toast.makeText(this, "El PIN debe tener 4 números", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            tvEstadoTitulo.text = "Vas de camino a la plaza"
            tvDistanciaInfo.text = "Dale este PIN al vendedor al llegar"

            // Mostramos el PIN gigante al comprador
            etPinVendedor.visibility = View.GONE
            tvPinComprador.visibility = View.VISIBLE
            tvPinComprador.text = "Tu PIN secreto: ${intercambio?.codigoVerificacion ?: "----"}"

            btnAccion.text = "CERRAR MAPA"
            btnAccion.setOnClickListener { finish() }
        }
    }

    private fun cargarDatosVehiculo() {
        val idCocheAMostrar = intercambio?.idCocheComprador ?: return

        RetrofitClient.getApiService().obtenerDetalleCoche(idCocheAMostrar).enqueue(object : Callback<Coche> {
            override fun onResponse(call: Call<Coche>, response: Response<Coche>) {
                if (response.isSuccessful && response.body() != null) {
                    val c = response.body()!!
                    tvCocheInfo.text = "${c.marca} ${c.modelo} • ${c.matricula}"
                }
            }
            override fun onFailure(call: Call<Coche>, t: Throwable) {
                tvCocheInfo.text = "Vehículo en camino"
            }
        })
    }

    private fun completarIntercambioConPin(pin: String) {
        val idIntercambio = intercambio?.id ?: return

        btnAccion.isEnabled = false
        btnAccion.text = "Validando..."

        RetrofitClient.getApiService().completarIntercambio(idIntercambio, pin).enqueue(object : Callback<Intercambio> {
            override fun onResponse(call: Call<Intercambio>, response: Response<Intercambio>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@RadarActivity, "¡Intercambio completado con éxito!", Toast.LENGTH_LONG).show()
                    finish() // Volvemos a la lista de reservas
                } else {
                    btnAccion.isEnabled = true
                    btnAccion.text = "VALIDAR PIN Y COBRAR"
                    Toast.makeText(this@RadarActivity, "PIN Incorrecto. Inténtalo de nuevo.", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<Intercambio>, t: Throwable) {
                btnAccion.isEnabled = true
                btnAccion.text = "VALIDAR PIN Y COBRAR"
                Toast.makeText(this@RadarActivity, "Error de red", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val posicionPlaza = LatLng(intercambio?.plazaLat ?: 0.0, intercambio?.plazaLong ?: 0.0)

        // Marcador estético
        mMap.addMarker(MarkerOptions()
            .position(posicionPlaza)
            .title("Punto de encuentro")
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)))

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(posicionPlaza, 17f))

        // Lanzar animación de pulso radar
        iniciarAnimacionRadar(posicionPlaza)
    }

    private fun iniciarAnimacionRadar(latLng: LatLng) {
        val circle = mMap.addCircle(CircleOptions()
            .center(latLng)
            .radius(0.0)
            .strokeWidth(2f)
            .strokeColor(Color.parseColor("#4285F4"))
            .fillColor(Color.parseColor("#224285F4")))

        val animator = ValueAnimator.ofFloat(0f, 150f)
        animator.repeatCount = ValueAnimator.INFINITE
        animator.duration = 2000
        animator.addUpdateListener {
            val radius = it.animatedValue as Float
            circle.radius = radius.toDouble()
            val alpha = ((1 - radius / 150f) * 255).toInt()
            circle.strokeColor = Color.argb(alpha, 66, 133, 244)
            circle.fillColor = Color.argb(alpha / 4, 66, 133, 244)
        }
        animator.start()
    }
}