package com.example.tfg.network

import com.example.tfg.models.*;
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Path
import retrofit2.http.DELETE
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Field
import retrofit2.Response
import okhttp3.ResponseBody

interface ApiService {
    @POST("api/usuarios/registro")
    fun registrarUsuario(@Body usuario: Usuario): Call<AuthResponse>

    @POST("api/usuarios/login")
    fun login(@Body usuario: Usuario): Call<AuthResponse>

    @POST("api/usuarios/google-login")
    fun loginConGoogle(@Body usuario: Usuario): Call<AuthResponse>

    @retrofit2.http.PUT("api/usuarios/actualizar-fcm")
    suspend fun actualizarFcm(@Query("token") token: String): Response<Unit>

    // Esta ya estaba bien configurada (Actualizada con filtros)
    @GET("api/intercambios/disponibles")
    fun obtenerIntercambiosDisponibles(@Query("idUsuarioConsulta") idUsuarioConsulta: Long?): Call<List<Intercambio>>

    @GET("api/intercambios/{id}")
    fun getIntercambioById(@Path("id") id: Long): Call<Intercambio>

    @POST("api/intercambios/ofrecer")
    fun ofrecerPlaza(@Body intercambio: Intercambio): Call<Intercambio>

    // Guardar un coche nuevo
    @POST("api/coches/registrar")
    fun registrarCoche(@Body coche: Coche): Call<Coche>

    // Obtener los coches de un usuario concreto
    @GET("api/coches/mis-coches")
    fun obtenerMisCoches(@Query("uid") uid: Long): Call<List<Coche>>

    // Usa Object o crea clases específicas para el mapeo
    @GET("api_ext/coches/marcas")
    fun obtenerMarcasApi(): Call<RespuestaMarcas>

    @GET("api_ext/coches/modelos/{marca}")
    fun obtenerModelosApi(@Path("marca") marca: String): Call<RespuestaModelos>

    @DELETE("api/coches/eliminar/{cid}")
    fun eliminarCoche(@Path("cid") cid: Long): Call<ResponseBody>


    @GET("api/intercambios/mis-ofrecidas/{idVendedor}")
    fun obtenerMisOfrecidas(@Path("idVendedor") idVendedor: Long): Call<List<Intercambio>>

    @GET("api/intercambios/mis-reservas/{idComprador}")
    fun obtenerMisReservas(@Path("idComprador") idComprador: Long): Call<List<Intercambio>>

    @GET("api/coches/detalle/{id}")
    fun obtenerDetalleCoche(@Path("id") id: Long): Call<Coche>

    @POST("api/intercambios/completar/{id}")
    fun completarIntercambio(
        @Path("id") id: Long,
        @Query("pinIngresado") pinIngresado: String
    ): Call<Intercambio>

    // --- NUEVOS ENDPOINTS DE STRIPE ---
    @POST("api/pagos/crear-payment-sheet")
    fun crearPaymentSheet(@Body data: PagoRequest): Call<PagoResponse>

    // Para que el vendedor cree su cuenta de Stripe
    @POST("api/pagos/crear-cuenta-vendedor/{idUsuario}")
    suspend fun crearCuentaVendedor(@Path("idUsuario") idUsuario: Long): Response<Map<String, String>>

    // Actualizamos reservar para enviar el paymentIntentId
    @FormUrlEncoded
    @POST("api/intercambios/reservar/{id}")
    suspend fun reservarPlaza(
        @Path("id") id: Long,
        @Field("idComprador") idComprador: Long,
        @Field("idCocheComprador") idCocheComprador: Long,
        @Field("paymentIntentId") paymentIntentId: String // El ID que nos dio Stripe
    ): Response<Intercambio>

    @retrofit2.http.PATCH("api/intercambios/{id}/calificar")
    suspend fun calificarIntercambio(
        @Path("id") id: Long,
        @Body request: CalificacionRequest
    ): Response<Intercambio>
}