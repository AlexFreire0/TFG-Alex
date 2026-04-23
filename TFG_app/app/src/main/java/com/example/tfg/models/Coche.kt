package com.example.tfg.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Coche(
    @SerializedName("cid")
    val cid: Long? = null,

    @SerializedName("dueno")
    val dueno: Usuario? = null,

    @SerializedName("marca")
    val marca: String? = "Desconocida",

    @SerializedName("modelo")
    val modelo: String? = "Desconocido",

    @SerializedName("matricula")
    val matricula: String? = "Sin matrícula",

    @SerializedName("color")
    val color: String? = "No especificado",

    @SerializedName("imagenUrl")
    val imagenUrl: String? = null,

    @SerializedName("activo")
    val activo: Boolean? = true,

    @SerializedName("capacidad")
    val capacidad: String? = null
) : Parcelable