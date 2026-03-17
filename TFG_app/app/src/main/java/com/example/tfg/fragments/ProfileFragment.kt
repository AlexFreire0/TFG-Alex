package com.example.tfg.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.tfg.R
import com.example.tfg.activities.CarteraActivity
import com.example.tfg.activities.LoginActivity
import com.example.tfg.databinding.FragmentProfileBinding
import com.example.tfg.utils.SessionManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Recuperar datos del usuario de SharedPreferences
        cargarDatosUsuario()

        // 2. Configurar listeners de la cuadrícula de acciones rápidas
        configurarBotonesGrid()

        // 3. Configurar listeners de las tarjetas de acción grandes
        configurarTarjetasAccion()
        
        // 4. Configurar listener del botón de cerrar sesión
        configurarBotonCerrarSesion()
    }

    private fun cargarDatosUsuario() {
        val sharedPref = requireActivity().getSharedPreferences("PreferenciasUsuario", Context.MODE_PRIVATE)
        val nombre = sharedPref.getString("nombre_usuario", "Nombre Apellido")
        val valoracion = sharedPref.getString("valoracion_usuario", "5.0")

        binding.tvUserName.text = nombre
        binding.tvUserRating.text = valoracion
    }

    private fun configurarBotonesGrid() {
        // Botón 1: Ayuda
        binding.cardHelp.setOnClickListener {
            mostrarToast("Ayuda en construcción")
        }

        // Botón 2: Monedero (Abre CarteraActivity)
        binding.cardWallet.setOnClickListener {
            val intent = Intent(requireContext(), CarteraActivity::class.java)
            startActivity(intent)
        }

        // Botón 3: Ajustes
        binding.cardSettings.setOnClickListener {
            mostrarToast("Ajustes en construcción")
        }

        // Botón 4: Compartir
        binding.cardShare.setOnClickListener {
            mostrarToast("Compartir en construcción")
        }
    }

    private fun configurarTarjetasAccion() {
        // Tarjeta 1: Mis coches
        binding.cardMyCars.setOnClickListener {
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, MisCochesFragment())
            transaction.addToBackStack(null)
            transaction.commit()
        }

        // Tarjeta 2: Mis plazas
        binding.cardMySpaces.setOnClickListener {
            mostrarToast("Abriendo Mis plazas...")
        }

        // Tarjeta 3: Seguridad
        binding.cardSecurity.setOnClickListener {
            mostrarToast("Abriendo Seguridad de la cuenta...")
        }
    }

    private fun mostrarToast(mensaje: String) {
        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
    }

    private fun configurarBotonCerrarSesion() {
        binding.btnLogout.setOnClickListener {
            // Configurar opciones para obtener el cliente de Google
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()
            val googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

            // 1. Cerrar la sesión nativa de Google
            googleSignInClient.signOut().addOnCompleteListener {
                // 2. Limpiar la sesión usando el SessionManager
                SessionManager.cerrarSesion(requireContext())
                
                // 3. Navegar al LoginActivity
                val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                    // Limpiar la pila de actividades
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
