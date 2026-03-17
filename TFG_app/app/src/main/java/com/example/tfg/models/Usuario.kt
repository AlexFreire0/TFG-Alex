package com.example.tfg.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Usuario(
    val uid: Long? = null,
    val googleId: String? = null,
    val nombre: String,
    val correo: String,
    val contrasenaHash: String? = null,
    val rol: String = "usuario",
    val activo: Boolean = true,
    // --- NUEVOS CAMPOS STRIPE ---
    val stripeCustomerId: String? = null,
    val stripeConnectId: String? = null
) : Parcelable