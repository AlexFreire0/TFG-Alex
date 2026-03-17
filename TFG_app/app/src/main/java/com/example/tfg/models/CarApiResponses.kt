package com.example.tfg.models

import com.google.gson.annotations.SerializedName

// Clase para la respuesta de MARCAS
data class RespuestaMarcas(
    @SerializedName("Count") val count: Int,
    @SerializedName("Message") val message: String,
    @SerializedName("Results") val Results: List<MarcaApi>
)

data class MarcaApi(
    @SerializedName("Make_ID") val Make_ID: Int,
    @SerializedName("Make_Name") val Make_Name: String
)

// Clase para la respuesta de MODELOS
data class RespuestaModelos(
    @SerializedName("Count") val count: Int,
    @SerializedName("Message") val message: String,
    @SerializedName("Results") val Results: List<ModeloApi>
)

data class ModeloApi(
    @SerializedName("Make_ID") val Make_ID: Int,
    @SerializedName("Make_Name") val Make_Name: String,
    @SerializedName("Model_ID") val Model_ID: Int,
    @SerializedName("Model_Name") val Model_Name: String
)