package com.example.tfg.models


import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PagoRequest(
    val precioTotal: Double,
    val idUsuario: Long,
    val idVendedor: Long
) : Parcelable