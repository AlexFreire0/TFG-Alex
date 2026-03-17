package com.example.tfg.activities;

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.example.tfg.databinding.ActivitySplashBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.tfg.activities.*;

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
            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
            finish()
        }
    }
}