package com.example.tfg

import android.app.Application
import com.example.tfg.network.RetrofitClient

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inicializar RetrofitClient globally para que AuthInterceptor pueda usar SharedPreferences
        RetrofitClient.init(this)
    }
}
