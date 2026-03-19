package com.example.tfg.network

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "http://192.168.1.100:8085/"

    private var retrofit: Retrofit? = null

    // Necesitamos inicializarlo con Context para que AuthInterceptor pueda leer de SharedPreferences
    fun init(context: Context) {
        if (retrofit == null) {
            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(context))
                // Configuramos un pequeño timeout
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
    }

    fun getApiService(): ApiService {
        return retrofit?.create(ApiService::class.java)
            ?: throw IllegalStateException("RetrofitClient no ha sido inicializado. Llama a init(context) en onCreate del Application o Activity.")
    }
}