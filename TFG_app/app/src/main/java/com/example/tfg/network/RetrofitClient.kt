package com.example.tfg.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://192.168.1.100:8085/" // Tu IP real

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Esta es la función que llama el Login y el Maps
    fun getApiService(): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}