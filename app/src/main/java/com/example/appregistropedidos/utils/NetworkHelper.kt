package com.example.appregistropedidos.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Utilidad para verificar la conectividad a internet.
 * Funciona con WiFi, datos móviles y Ethernet.
 */
object NetworkHelper {

    /**
     * Verifica si el dispositivo tiene conexión a internet activa.
     * Retorna true si hay WiFi, datos móviles o Ethernet disponible.
     * Retorna false si está en modo avión o sin señal.
     */
    fun hayConexion(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val red = connectivityManager.activeNetwork ?: return false
        val capacidades = connectivityManager.getNetworkCapabilities(red) ?: return false

        return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) &&
               (capacidades.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capacidades.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capacidades.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
    }
}
