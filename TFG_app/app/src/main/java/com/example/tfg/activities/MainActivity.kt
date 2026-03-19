package com.example.tfg.activities

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.tfg.databinding.ActivitySplashBinding
import com.example.tfg.utils.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    companion object {
        const val TIEMPO_CARGA = 3000L
    }
    
    private lateinit var binding: ActivitySplashBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            delay(TIEMPO_CARGA)
            
            val token = SessionManager.getToken(this@MainActivity)
            
            if (isTokenValid(token)) {
                // Token válido: Redirigimos al menú principal con los datos cacheados
                val intent = Intent(this@MainActivity, InicioActivity::class.java)
                val usuarioLogueado = SessionManager.getUsuarioLogueado(this@MainActivity)
                intent.putExtra("USER_DATA", usuarioLogueado)
                startActivity(intent)
            } else {
                // Token expirado o inexistente: Limpiamos por si acaso y redirigimos al Login
                SessionManager.cerrarSesion(this@MainActivity)
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
            }
            finish()
        }
    }

    /**
     * Decodifica el payload del JWT (Base64 URL Safe) y comprueba el claim 'exp'.
     * No necesitamos verificar la firma aquí, el backend lo hará.
     * Solo necesitamos saber si a nivel local ya expiró.
     */
    private fun isTokenValid(token: String?): Boolean {
        if (token.isNullOrEmpty()) return false
        try {
            val parts = token.split(".")
            if (parts.size != 3) return false
            
            val payloadBase64 = parts[1]
            val payloadString = String(Base64.decode(payloadBase64, Base64.URL_SAFE))
            
            val jsonObject = JSONObject(payloadString)
            val exp = jsonObject.getLong("exp")
            val currentTimeInSeconds = System.currentTimeMillis() / 1000
            
            return exp > currentTimeInSeconds
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}