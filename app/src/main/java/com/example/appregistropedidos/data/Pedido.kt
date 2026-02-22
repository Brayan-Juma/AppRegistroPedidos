package com.example.appregistropedidos.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pedidos")
data class Pedido(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val vendedor: String,      // Usuario que registró el pedido
    val nombreCliente: String,
    val telefono: String,
    val direccion: String,
    val detalle: String,
    val tipoPago: String,       // "Efectivo" o "Transferencia"
    val fotoPath: String,       // Ruta local de la imagen capturada
    val latitud: Double,
    val longitud: Double,
    val estado: String = "Pendiente",
    val fecha: Long,             // Timestamp de creación
    val remoteId: String? = null // ID asignado por MockAPI (nulo si no sincronizado)
)
