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

class InicioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInicioBinding
    private var usuario: Usuario? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Activar el modo de borde a borde (Imprescindible para tablets modernas)
        enableEdgeToEdge()

        binding = ActivityInicioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2. SOLUCIÓN TABLET: Ajustar márgenes para que los botones del sistema no tapen la barra
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavigation) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            // Aplicamos el padding inferior dinámico según el tamaño de los botones de la tablet
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        // 3. Recuperar los datos del usuario
        usuario = intent.getParcelableExtra<Usuario>("USER_DATA")

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
}