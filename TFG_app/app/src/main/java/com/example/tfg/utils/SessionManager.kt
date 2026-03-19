package com.example.tfg.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.tfg.models.Usuario

object SessionManager {

    private const val PREF_NAME = "TFG_Session"
    private const val KEY_UID = "UID"
    private const val KEY_NOMBRE = "NOMBRE"
    private const val KEY_CORREO = "CORREO"
    private const val KEY_TOKEN = "TOKEN"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Guarda los datos importantes del usuario tras un login exitoso.
     */
    fun guardarSesion(context: Context, token: String?, usuario: Usuario?) {
        if (token.isNullOrEmpty() || usuario == null) {
            android.util.Log.e("SessionManager", "Error: Se intentó guardar sesión con token nulo o usuario nulo")
            return
        }
        val editor = getPreferences(context).edit()
        usuario.uid?.let { editor.putLong(KEY_UID, it) }
        editor.putString(KEY_NOMBRE, usuario.nombre)
        editor.putString(KEY_CORREO, usuario.correo)
        editor.putString(KEY_TOKEN, token)
        editor.apply()
    }

    /**
     * Recupera el token JWT.
     */
    fun getToken(context: Context): String? {
        return getPreferences(context).getString(KEY_TOKEN, null)
    }

    /**
     * Recupera el ID del usuario logueado.
     * @return El ID del usuario, o -1 si no hay sesión.
     */
    fun getUsuarioId(context: Context): Long {
        return getPreferences(context).getLong(KEY_UID, -1L)
    }

    /**
     * Recupera un objeto Usuario con los datos básicos de la sesión actual.
     * Útil si necesitas pasar el objeto entero por nav arguments o endpoints.
     */
    fun getUsuarioLogueado(context: Context): Usuario? {
        val prefs = getPreferences(context)
        val uid = prefs.getLong(KEY_UID, -1L)
        if (uid == -1L) return null

        val nombre = prefs.getString(KEY_NOMBRE, "") ?: ""
        val correo = prefs.getString(KEY_CORREO, "") ?: ""

        return Usuario(uid = uid, nombre = nombre, correo = correo)
    }

    /**
     * Limpia las preferencias (para el botón de Logout o Sesión Expirada).
     */
    fun cerrarSesion(context: Context) {
        getPreferences(context).edit().clear().apply()
    }

    /**
     * Comprueba si un JWT ha expirado decodificando el payload.
     * @return true si el token está expirado o es inválido, false si es válido.
     */
    fun isTokenExpired(token: String): Boolean {
        try {
            val parts = token.split(".")
            if (parts.size != 3) return true
            
            val payload = String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE), Charsets.UTF_8)
            val jsonObject = org.json.JSONObject(payload)
            
            if (jsonObject.has("exp")) {
                val exp = jsonObject.getLong("exp")
                // exp está en segundos, el sistema Android en milisegundos
                return (exp * 1000) < System.currentTimeMillis()
            }
            return true // Si el JWT no tiene 'exp', lo tratamos como inválido
        } catch (e: Exception) {
            android.util.Log.e("SessionManager", "Error al decodificar JWT: ${e.message}")
            return true
        }
    }
}
