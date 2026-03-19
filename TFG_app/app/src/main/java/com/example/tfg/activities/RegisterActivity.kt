package com.example.tfg.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tfg.databinding.ActivityRegisterBinding
import com.example.tfg.models.Usuario
import com.example.tfg.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvLogin.setOnClickListener {
            finish()
        }

        binding.btnRegister.setOnClickListener {
            registrarUsuario()
        }
    }

    private fun registrarUsuario() {
        val nombre = binding.etName.text.toString().trim()
        val correo = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        if (nombre.isEmpty() || correo.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            return
        }

        val usuario = Usuario(
            nombre = nombre,
            correo = correo,
            contrasenaHash = password
        )

        // CORRECCIÓN: Llamamos directamente a getApiService() sin usar .instance
        val apiService = RetrofitClient.getApiService()

        apiService.registrarUsuario(usuario).enqueue(object : Callback<com.example.tfg.models.AuthResponse> {
            override fun onResponse(call: Call<com.example.tfg.models.AuthResponse>, response: Response<com.example.tfg.models.AuthResponse>) {
                if (response.isSuccessful) {
                    val authResponse = response.body()
                    if (authResponse != null) {
                        // Opcional: Auto-login guardando la sesión aquí. Si no, el usuario tendrá que hacer login manual.
                        // com.example.tfg.utils.SessionManager.guardarSesion(this@RegisterActivity, authResponse.token, authResponse.usuario)
                    }
                    Toast.makeText(this@RegisterActivity, "Registro exitoso", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@RegisterActivity, "Error en el registro: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.example.tfg.models.AuthResponse>, t: Throwable) {
                Toast.makeText(this@RegisterActivity, "Error de conexión: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}