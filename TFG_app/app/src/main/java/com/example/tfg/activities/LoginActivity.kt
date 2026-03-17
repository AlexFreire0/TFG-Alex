package com.example.tfg.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tfg.databinding.ActivityLoginBinding
import com.example.tfg.models.Usuario
import com.example.tfg.network.RetrofitClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- 1. CONFIGURACIÓN DE GOOGLE ---
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("708691945915-kmbqn1n1tkk937f49lorc42l8abd54o0.apps.googleusercontent.com")
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        mGoogleSignInClient.signOut()

        // --- 2. BOTONES ---
        binding.btnGoogle.setOnClickListener {
            mGoogleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = mGoogleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_SIGN_IN)
            }
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val usuario = Usuario(nombre = "", correo = email, contrasenaHash = password)
            realizarLoginBackend(usuario)
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                loginConGoogleEnBackend(account)
            } catch (e: ApiException) {
                Log.e("GOOGLE_ERROR", "Código: ${e.statusCode}")
                Toast.makeText(this, "Error Google (${e.statusCode})", Toast.LENGTH_SHORT).show()
                mGoogleSignInClient.signOut()
            }
        }
    }

    private fun loginConGoogleEnBackend(account: GoogleSignInAccount) {
        val usuarioGoogle = Usuario(
            nombre = account.displayName ?: "",
            correo = account.email ?: "",
            googleId = account.id
        )

        val apiService = RetrofitClient.getApiService()

        apiService.loginConGoogle(usuarioGoogle).enqueue(object : Callback<Usuario> {
            override fun onResponse(call: Call<Usuario>, response: Response<Usuario>) {
                if (response.isSuccessful) {
                    val usuarioBackend = response.body()
                    if (usuarioBackend != null) {
                        // d) SOLO DENTRO del onResponse exitoso de Retrofit usar el usuario devuelto
                        com.example.tfg.utils.SessionManager.guardarSesion(this@LoginActivity, usuarioBackend)
                        
                        // e) Después de guardar la sesión, hacer el startActivity y finish
                        val intent = Intent(this@LoginActivity, InicioActivity::class.java)
                        intent.putExtra("USER_DATA", usuarioBackend)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    Toast.makeText(this@LoginActivity, "Error servidor: ${response.code()}", Toast.LENGTH_SHORT).show()
                    mGoogleSignInClient.signOut()
                }
            }
            override fun onFailure(call: Call<Usuario>, t: Throwable) {
                Log.e("RETROFIT_ERROR", t.message ?: "Error desconocido")
                Toast.makeText(this@LoginActivity, "Sin conexión", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun realizarLoginBackend(usuario: Usuario) {
        val apiService = RetrofitClient.getApiService()

        apiService.login(usuario).enqueue(object : Callback<Usuario> {
            override fun onResponse(call: Call<Usuario>, response: Response<Usuario>) {
                if (response.isSuccessful) {
                    val usuarioBackend = response.body()
                    if (usuarioBackend != null) {
                        // Guardar la sesión con el usuario validado
                        com.example.tfg.utils.SessionManager.guardarSesion(this@LoginActivity, usuarioBackend)
                        
                        // Navegar a la pantalla principal
                        val intent = Intent(this@LoginActivity, InicioActivity::class.java)
                        intent.putExtra("USER_DATA", usuarioBackend)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    Toast.makeText(this@LoginActivity, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<Usuario>, t: Throwable) {
                Toast.makeText(this@LoginActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}