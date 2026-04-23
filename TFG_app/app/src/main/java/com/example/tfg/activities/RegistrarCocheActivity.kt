package com.example.tfg.activities

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.tfg.databinding.ActivityRegistrarCocheBinding
import com.example.tfg.models.Coche
import com.example.tfg.models.RespuestaMarcas
import com.example.tfg.models.RespuestaModelos
import com.example.tfg.models.Usuario
import com.example.tfg.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegistrarCocheActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistrarCocheBinding
    private var usuarioActual: Usuario? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegistrarCocheBinding.inflate(layoutInflater)
        setContentView(binding.root)

        usuarioActual = intent.getParcelableExtra("USER_DATA")

        val capacidades = arrayOf("Moto-Bici", "Coche pequeño", "Coche mediano", "Coche grande", "Furgoneta", "Vehículo muy grande")
        val adapterCapacidad = ArrayAdapter(this, android.R.layout.simple_list_item_1, capacidades)
        binding.spinnerCapacidad.setAdapter(adapterCapacidad)

        cargarMarcasDesdeAPI()

        // Listener para cargar modelos al seleccionar una marca
        binding.spinnerMarca.setOnItemClickListener { parent, _, position, _ ->
            val marcaSeleccionada = parent.getItemAtPosition(position).toString()
            cargarModelosDesdeAPI(marcaSeleccionada)
        }

        binding.btnGuardarCoche.setOnClickListener {
            enviarCocheAlServidor()
        }
    }

    private fun cargarMarcasDesdeAPI() {
        RetrofitClient.getApiService().obtenerMarcasApi().enqueue(object : Callback<RespuestaMarcas> {
            override fun onResponse(call: Call<RespuestaMarcas>, response: Response<RespuestaMarcas>) {
                if (response.isSuccessful && response.body() != null) {
                    val marcas = response.body()!!.Results.map { it.Make_Name }.sorted()
                    val adapter = ArrayAdapter(this@RegistrarCocheActivity, android.R.layout.simple_list_item_1, marcas)
                    binding.spinnerMarca.setAdapter(adapter)
                }
            }
            override fun onFailure(call: Call<RespuestaMarcas>, t: Throwable) {
                Toast.makeText(this@RegistrarCocheActivity, "Error al cargar marcas", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun cargarModelosDesdeAPI(marca: String) {
        binding.spinnerModelo.setText("")
        binding.layoutModelo.isEnabled = false

        RetrofitClient.getApiService().obtenerModelosApi(marca).enqueue(object : Callback<RespuestaModelos> {
            override fun onResponse(call: Call<RespuestaModelos>, response: Response<RespuestaModelos>) {
                if (response.isSuccessful && response.body() != null) {
                    val modelos = response.body()!!.Results.map { it.Model_Name }.sorted()
                    val adapter = ArrayAdapter(this@RegistrarCocheActivity, android.R.layout.simple_list_item_1, modelos)
                    binding.spinnerModelo.setAdapter(adapter)
                    binding.layoutModelo.isEnabled = true
                }
            }
            override fun onFailure(call: Call<RespuestaModelos>, t: Throwable) {
                Toast.makeText(this@RegistrarCocheActivity, "Error al cargar modelos", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun enviarCocheAlServidor() {
        val marca = binding.spinnerMarca.text.toString().trim()
        val modelo = binding.spinnerModelo.text.toString().trim()
        val matriculaInput = binding.etMatricula.text.toString().trim().uppercase()
        val capacidad = binding.spinnerCapacidad.text.toString().trim()

        // 1. Limpiamos espacios/guiones y definimos el Regex de validación
        val matriculaLimpia = matriculaInput.replace(" ", "").replace("-", "")
        val matriculaRegex = "^([0-9]{4}[BCDFGHJKLMNPQRSTVWXYZ]{3}|[A-Z]{1,2}[0-9]{4,6}[A-Z]{0,2})$".toRegex()

        // 2. Validaciones de campos vacíos y formato de matrícula
        if (marca.isEmpty() || modelo.isEmpty() || matriculaLimpia.isEmpty() || capacidad.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (!matriculaLimpia.matches(matriculaRegex)) {
            binding.etMatricula.error = "Formato incorrecto (Ej: 1234BBB o M1234AZ)"
            return
        }

        if (usuarioActual == null || usuarioActual?.uid == null) {
            Toast.makeText(this, "Sesión no válida", Toast.LENGTH_SHORT).show()
            return
        }

        // Usuario simplificado para evitar errores de duplicidad en el servidor
        val duenoSimplificado = Usuario(
            uid = usuarioActual!!.uid,
            nombre = "",
            correo = ""
        )

        val nuevoCoche = Coche(
            cid = 0, // El servidor lo pondrá a null o ignorará para crear uno nuevo
            dueno = duenoSimplificado,
            marca = marca,
            modelo = modelo,
            matricula = matriculaLimpia,
            color = "No especificado",
            activo = true,
            capacidad = capacidad
        )

        RetrofitClient.getApiService().registrarCoche(nuevoCoche).enqueue(object : Callback<Coche> {
            override fun onResponse(call: Call<Coche>, response: Response<Coche>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@RegistrarCocheActivity, "¡Vehículo guardado!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                    Toast.makeText(this@RegistrarCocheActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<Coche>, t: Throwable) {
                Toast.makeText(this@RegistrarCocheActivity, "Fallo de conexión", Toast.LENGTH_SHORT).show()
            }
        })
    }
}