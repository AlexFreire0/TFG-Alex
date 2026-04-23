package com.example.tfg.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.google.gson.annotations.SerializedName

@Parcelize
data class Intercambio(
    val id: Long? = null,
    val idVendedor: Long,
    val idComprador: Long? = null,
    val idCocheVendedor: Long,
    val idCocheComprador: Long? = null,

    val precioTotalComprador: Double,
    val comisionServicio: Double,
    val gananciaVendedor: Double,

    val momentoIntercambioPrevisto: String,
    val cortesiaMinutos: Int = 5,

    val plazaLat: Double,
    val plazaLong: Double,
    val plazaDireccionTexto: String? = null,
    val capacidad: String? = null,

    val estadoIntercambio: String = "Esperando",
    val estadoResultado: String? = null,

    @SerializedName("codigoVerificacion")
    val codigoVerificacion: String? = null,

    @SerializedName("calificacionAlVendedor")
    val calificacionAlVendedor: Int? = null,

    @SerializedName("calificacionAlComprador")
    val calificacionAlComprador: Int? = null,

    @SerializedName("observacionesDelComprador")
    val observacionesDelComprador: String? = null,

    @SerializedName("observacionesDelVendedor")
    val observacionesDelVendedor: String? = null
) : Parcelable