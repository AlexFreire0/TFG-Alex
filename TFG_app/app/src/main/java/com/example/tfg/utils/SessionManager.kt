package com.example.tfg.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.tfg.models.Usuario

object SessionManager {

    private const val PREF_NAME = "TFG_Session"
    private const val KEY_UID = "UID"
    private const val KEY_NOMBRE = "NOMBRE"
    private const val KEY_CORREO = "CORREO"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Guarda los datos importantes del usuario tras un login exitoso.
     */
    fun guardarSesion(context: Context, usuario: Usuario) {
        val editor = getPreferences(context).edit()
        usuario.uid?.let { editor.putLong(KEY_UID, it) }
        editor.putString(KEY_NOMBRE, usuario.nombre)
        editor.putString(KEY_CORREO, usuario.correo)
        editor.apply()
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
     * Limpia las preferencias (para el botón de Logout).
     */
    fun cerrarSesion(context: Context) {
        getPreferences(context).edit().clear().apply()
    }
}
