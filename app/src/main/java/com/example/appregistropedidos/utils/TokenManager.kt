package com.example.appregistropedidos.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Gestión del token de autenticación usando SharedPreferences.
 * Guarda y recupera el token JWT recibido de POST /auth/login.
 */
object TokenManager {

    private const val PREF_NAME = "auth_prefs"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_VENDEDOR = "auth_vendedor"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Guarda el token recibido del login.
     */
    fun guardarSesion(context: Context, token: String, vendedor: String) {
        getPrefs(context).edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_VENDEDOR, vendedor)
            .apply()
    }

    /**
     * Obtiene el token guardado, o null si no existe.
     */
    fun obtenerToken(context: Context): String? {
        return getPrefs(context).getString(KEY_TOKEN, null)
    }

    /**
     * Obtiene el nombre del vendedor logueado.
     */
    fun obtenerVendedor(context: Context): String {
        return getPrefs(context).getString(KEY_VENDEDOR, "Vendedor Desconocido") ?: "Vendedor Desconocido"
    }

    /**
     * Retorna el header "Bearer <token>" listo para usar en Authorization.
     */
    fun obtenerBearerToken(context: Context): String? {
        val token = obtenerToken(context) ?: return null
        return "Bearer $token"
    }

    /**
     * Elimina el token (logout).
     */
    fun eliminarToken(context: Context) {
        getPrefs(context).edit().remove(KEY_TOKEN).apply()
    }

    /**
     * Verifica si hay un token guardado (sesión activa).
     */
    fun haySession(context: Context): Boolean {
        return obtenerToken(context) != null
    }
}
