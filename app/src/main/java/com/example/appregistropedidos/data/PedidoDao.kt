package com.example.appregistropedidos.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PedidoDao {

    @Insert
    suspend fun insertarPedido(pedido: Pedido)

    @Query("SELECT * FROM pedidos WHERE vendedor = :vendedor ORDER BY fecha DESC")
    suspend fun obtenerPedidosPorVendedor(vendedor: String): List<Pedido>

    @Query("SELECT * FROM pedidos ORDER BY fecha DESC")
    suspend fun obtenerTodosLosPedidos(): List<Pedido>

    @Query("UPDATE pedidos SET estado = :nuevoEstado WHERE id = :id")
    suspend fun actualizarEstado(id: Int, nuevoEstado: String)

    @Query("SELECT * FROM pedidos WHERE estado = 'Pendiente' AND vendedor = :vendedor ORDER BY fecha DESC")
    suspend fun obtenerPedidosPendientesPorVendedor(vendedor: String): List<Pedido>

    @Query("SELECT * FROM pedidos WHERE estado = 'Pendiente' ORDER BY fecha DESC")
    suspend fun obtenerPedidosPendientes(): List<Pedido>

    @Query("SELECT * FROM pedidos WHERE remoteId = :remoteId LIMIT 1")
    suspend fun buscarPorRemoteId(remoteId: String): Pedido?

    @Query("UPDATE pedidos SET estado = :nuevoEstado, remoteId = :remoteId WHERE id = :id")
    suspend fun marcarSincronizado(id: Int, nuevoEstado: String, remoteId: String)
}
