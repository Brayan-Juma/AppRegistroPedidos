package com.example.appregistropedidos.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Singleton para configurar y proveer la instancia de Retrofit.
 *
 * IMPORTANTE: Cambiar BASE_URL por la URL real del servidor antes de desplegar.
 */
object RetrofitClient {

    // URL base de la API — cambiar según el entorno
    private const val BASE_URL = "https://6997d3a4d66520f95f15ca6b.mockapi.io/"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}
