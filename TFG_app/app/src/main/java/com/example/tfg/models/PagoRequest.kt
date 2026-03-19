package com.example.tfg.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PagoRequest(
    val idIntercambio: Long,
    val idUsuario: Long,
    val idVendedor: Long
) : Parcelable