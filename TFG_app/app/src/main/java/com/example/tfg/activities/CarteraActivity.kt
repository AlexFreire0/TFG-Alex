package com.example.tfg.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.tfg.R
import com.example.tfg.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CarteraActivity : AppCompatActivity() {

    private lateinit var tvConfigurarStripe: TextView
    private lateinit var btnRetirarSaldo: Button
    private lateinit var progressBarStripe: ProgressBar
    private lateinit var tvSaldoAmount: TextView
    private var idUsuario: Long = -1L
    private var saldoActual: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cartera)

        tvConfigurarStripe = findViewById(R.id.tvConfigurarStripe)
        btnRetirarSaldo = findViewById(R.id.btnRetirarSaldo)
        progressBarStripe = findViewById(R.id.progressBarStripe)
        tvSaldoAmount = findViewById(R.id.tvSaldoAmount)

        idUsuario = com.example.tfg.utils.SessionManager.getUsuarioId(this)

        // Botón de Retirar (Simulación)
        btnRetirarSaldo.setOnClickListener {
            if (saldoActual > 0) {
                Toast.makeText(this, "Simulando transferencia de ${String.format("%.2f€", saldoActual)} a tu cuenta...", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "No tienes saldo suficiente para retirar", Toast.LENGTH_SHORT).show()
            }
        }

        // Link de configuración de Stripe
        tvConfigurarStripe.setOnClickListener {
            configurarStripe()
        }

        if (idUsuario != -1L) {
            cargarSaldo()
        } else {
            Toast.makeText(this, "Error: Usuario no encontrado en preferencias", Toast.LENGTH_SHORT).show()
        }

        manejarDeepLink(intent)
    }

    override fun onNewIntent(newIntent: Intent) {
        super.onNewIntent(newIntent)
        setIntent(newIntent)
        manejarDeepLink(newIntent)
    }

    private fun manejarDeepLink(intent: Intent?) {
        val uri = intent?.data
        if (uri != null && uri.scheme == "parkinghole") {
            when (uri.host) {
                "onboarding-exito" -> {
                    Toast.makeText(this, "¡Cuenta de Stripe configurada con éxito!", Toast.LENGTH_LONG).show()
                }
                "onboarding-reintentar" -> {
                    Toast.makeText(this, "Configuración cancelada o incompleta. Inténtalo de nuevo.", Toast.LENGTH_LONG).show()
                }
            }
            // Limpiamos el intent para evitar que el Toast vuelva a salir al rotar pantalla
            setIntent(Intent())
        }
    }

    private fun cargarSaldo() {
        progressBarStripe.visibility = View.VISIBLE
        RetrofitClient.getApiService().obtenerSaldo(idUsuario).enqueue(object : Callback<Double> {
            override fun onResponse(call: Call<Double>, response: Response<Double>) {
                progressBarStripe.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    saldoActual = response.body()!!
                    tvSaldoAmount.text = String.format("%.2f€", saldoActual)
                } else {
                    Toast.makeText(this@CarteraActivity, "Error al cargar el saldo", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Double>, t: Throwable) {
                progressBarStripe.visibility = View.GONE
                Toast.makeText(this@CarteraActivity, "Error de red al conectar", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun configurarStripe() {
        if (idUsuario == -1L) {
            Toast.makeText(this, "Sesión no válida. Inicia sesión de nuevo.", Toast.LENGTH_LONG).show()
            return
        }

        // Disable interaction and show progress bar
        tvConfigurarStripe.isEnabled = false
        progressBarStripe.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.getApiService().crearCuentaVendedor(idUsuario)

                withContext(Dispatchers.Main) {
                    progressBarStripe.visibility = View.GONE
                    tvConfigurarStripe.isEnabled = true

                    if (response.isSuccessful) {
                        val body = response.body()
                        val url = body?.get("url")

                        if (!url.isNullOrEmpty()) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(this@CarteraActivity, "No se encontró un navegador web para abrir el enlace.", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(this@CarteraActivity, "La respuesta no contiene una URL válida.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@CarteraActivity, "Error del servidor: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBarStripe.visibility = View.GONE
                    tvConfigurarStripe.isEnabled = true
                    Toast.makeText(this@CarteraActivity, "Error de red: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
