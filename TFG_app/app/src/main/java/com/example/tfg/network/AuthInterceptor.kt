package com.example.tfg.network

import android.content.Context
import com.example.tfg.utils.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = SessionManager.getToken(context)
        val originalRequest = chain.request()

        // 1. Obtenemos la URL usando el método público .url()
        // 2. Extraemos el path para comprobar si es un endpoint público
        val requestUrl = originalRequest.url()
        val path = requestUrl.encodedPath()

        val isPublicEndpoint = path.contains("login") ||
                path.contains("registro") ||
                path.contains("webhook")

        return if (!token.isNullOrEmpty() && !isPublicEndpoint) {
            // Si tenemos token y no es un endpoint público, lo añadimos
            val authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
            chain.proceed(authenticatedRequest)
        } else {
            // Si no, enviamos la petición original (sin token o pública)
            chain.proceed(originalRequest)
        }
    }
}