package com.example.appregistropedidos.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

/**
 * Helper para obtener la ubicación actual usando FusedLocationProviderClient.
 * Usar al momento de guardar un pedido para capturar latitud y longitud.
 *
 * Ejemplo de uso:
 * ```
 * LocationHelper.obtenerUbicacionActual(context) { lat, lng ->
 *     // Usar lat y lng al crear el Pedido
 * }
 * ```
 */
object LocationHelper {

    /**
     * Obtiene la ubicación actual del dispositivo.
     *
     * @param context Contexto de la actividad o fragmento.
     * @param onUbicacionObtenida Callback con latitud y longitud. Si falla, devuelve (0.0, 0.0).
     */
    fun obtenerUbicacionActual(
        context: Context,
        onUbicacionObtenida: (latitud: Double, longitud: Double) -> Unit
    ) {
        // Verificar que los permisos de ubicación estén otorgados
        val tienePermisoFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val tienePermisoCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!tienePermisoFine && !tienePermisoCoarse) {
            // Sin permisos, devolver coordenadas por defecto
            onUbicacionObtenida(0.0, 0.0)
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val cancellationTokenSource = CancellationTokenSource()

        // Prioridad alta para obtener ubicación GPS precisa
        val priority = if (tienePermisoFine) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }

        fusedLocationClient.getCurrentLocation(priority, cancellationTokenSource.token)
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    onUbicacionObtenida(location.latitude, location.longitude)
                } else {
                    // Si no se pudo obtener la ubicación, intentar con la última conocida
                    fusedLocationClient.lastLocation
                        .addOnSuccessListener { lastLocation: Location? ->
                            onUbicacionObtenida(
                                lastLocation?.latitude ?: 0.0,
                                lastLocation?.longitude ?: 0.0
                            )
                        }
                        .addOnFailureListener {
                            onUbicacionObtenida(0.0, 0.0)
                        }
                }
            }
            .addOnFailureListener {
                onUbicacionObtenida(0.0, 0.0)
            }
    }
}
