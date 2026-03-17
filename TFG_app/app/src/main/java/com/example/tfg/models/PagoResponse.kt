package com.example.tfg.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PagoResponse(
    val paymentIntent: String,
    val ephemeralKey: String,
    val customer: String
) : Parcelable