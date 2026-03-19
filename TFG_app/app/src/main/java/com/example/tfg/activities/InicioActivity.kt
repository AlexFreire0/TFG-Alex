package com.example.tfg.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.tfg.R
import com.example.tfg.databinding.ActivityInicioBinding
import com.example.tfg.fragments.ProfileFragment
import com.example.tfg.fragments.PrincipalFragment
import com.example.tfg.fragments.ReservasFragment
import com.example.tfg.models.Usuario
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.google.firebase.messaging.FirebaseMessaging
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class InicioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInicioBinding
    private var usuario: Usuario? = null

    // Lanzador para solicitar permisos en Android moderno API 33+
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            android.util.Log.d("FCM_PERMISSION", "Permiso de notificaciones CONCEDIDO por el usuario.")
        } else {
            android.util.Log.w("FCM_PERMISSION", "Permiso de notificaciones RECHAZADO.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Activar el modo de borde a borde (Imprescindible para tablets modernas)
        enableEdgeToEdge()

        binding = ActivityInicioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1.5 Solicitar permisos de notificación en Android 13+
        checkNotificationPermission()

        // 2. SOLUCIÓN TABLET: Ajustar márgenes para que los botones del sistema no tapen la barra
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavigation) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            // Aplicamos el padding inferior dinámico según el tamaño de los botones de la tablet
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        // 3. Recuperar los datos del usuario
        usuario = intent.getParcelableExtra<Usuario>("USER_DATA")

        // 3.5 Petición del token FCM y actualización en el backend
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                android.util.Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            val fcmToken = task.result ?: return@addOnCompleteListener
            android.util.Log.d("FCM", "Token actual: $fcmToken")
            
            // Subir al backend usando corrutina (AuthInterceptor adjunta el JWT automáticamente)
            lifecycleScope.launch {
                try {
                    val response = com.example.tfg.network.RetrofitClient.getApiService().actualizarFcm(fcmToken)
                    android.util.Log.d("DEBUG_FCM", "Respuesta del servidor: ${response.code()}")
                    if (!response.isSuccessful) {
                        android.util.Log.e("DEBUG_FCM", "Error detallado: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DEBUG_FCM", "Excepción al enviar FCM token: ${e.message}", e)
                }
            }
        }

        // 4. Configurar el estado inicial
        if (savedInstanceState == null) {
            binding.bottomNavigation.selectedItemId = R.id.nav_principal
            cargarFragmento(PrincipalFragment())
        }

        // 5. Listener de la barra de navegación inferior
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_reservas -> {
                    cargarFragmento(ReservasFragment())
                    true
                }
                R.id.nav_principal -> {
                    cargarFragmento(PrincipalFragment())
                    true
                }
                R.id.nav_perfil -> {
                    val profileFrag = ProfileFragment()
                    cargarFragmento(profileFrag)
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Lógica para intercambiar los fragmentos en el FrameLayout
     */
    private fun cargarFragmento(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Ya está concedido
                    android.util.Log.d("FCM_PERMISSION", "Permiso ya concedido.")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // El usuario rechazó antes, pero no permanentemente. Le volvemos a preguntar.
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // Primera vez, preguntamos directamente
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}