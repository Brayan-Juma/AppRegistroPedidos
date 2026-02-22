package com.example.appregistropedidos.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Modelos para la API de autenticación y pedidos.
 */
data class LoginRequest(
    val usuario: String,
    val contrasena: String
)

data class LoginResponse(
    val usuario: String,
    val contrasena: String,
    val token: String,
    val mensaje: String? = null
)

data class PedidoRequest(
    val id: String? = null, // ID devuelto por MockAPI
    val vendedor: String,
    val nombreCliente: String,
    val telefono: String,
    val direccion: String,
    val detalle: String,
    val tipoPago: String,
    val fotoPath: String,
    val fotoBase64: String? = null, // Imagen en Base64 para sincronización
    val latitud: Double,
    val longitud: Double,
    val estado: String,
    val fecha: String
)

/**
 * Interfaz de la API REST para login y sincronización de pedidos.
 */
interface ApiService {

    @GET("auth")
    suspend fun login(
        @Query("usuario") usuario: String,
        @Query("contrasena") contrasena: String
    ): Response<List<LoginResponse>>

    @POST("orders")
    suspend fun enviarPedido(
        @Header("Authorization") token: String,
        @Body pedido: PedidoRequest
    ): Response<PedidoRequest>

    @GET("orders")
    suspend fun obtenerPedidosPorVendedor(
        @Query("vendedor") vendedor: String
    ): Response<List<PedidoRequest>>
}
