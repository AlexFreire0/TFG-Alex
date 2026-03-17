package com.example.tfg.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.tfg.R
import com.example.tfg.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CarteraActivity : AppCompatActivity() {

    private lateinit var btnConfigurarStripe: Button
    private lateinit var progressBarStripe: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cartera)

        btnConfigurarStripe = findViewById(R.id.btnConfigurarStripe)
        progressBarStripe = findViewById(R.id.progressBarStripe)

        btnConfigurarStripe.setOnClickListener {
            configurarStripe()
        }
    }

    private fun configurarStripe() {
        val prefs = getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val idUsuario = prefs.getLong("id", -1L)

        if (idUsuario == -1L) {
            Toast.makeText(this, "Error: Usuario no encontrado en preferencias", Toast.LENGTH_SHORT).show()
            return
        }

        // Disable button and show progress bar
        btnConfigurarStripe.isEnabled = false
        progressBarStripe.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Using getApiService() as that is how RetrofitClient is structured in this project
                val response = RetrofitClient.getApiService().crearCuentaVendedor(idUsuario)

                withContext(Dispatchers.Main) {
                    // Re-enable UI
                    progressBarStripe.visibility = View.GONE
                    btnConfigurarStripe.isEnabled = true

                    if (response.isSuccessful) {
                        val body = response.body()
                        val url = body?.get("url")

                        if (!url.isNullOrEmpty()) {
                            // Open Stripe URL in browser
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            startActivity(intent)
                        } else {
                            Toast.makeText(this@CarteraActivity, "La respuesta no contiene una URL válida", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@CarteraActivity, "Error del servidor: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBarStripe.visibility = View.GONE
                    btnConfigurarStripe.isEnabled = true
                    Toast.makeText(this@CarteraActivity, "Error de red: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
