package com.example.appregistropedidos.network

import retrofit2.Response

/**
 * Simulador local de la API REST para demostración.
 *
 * Credenciales de prueba:
 *   - Usuario: admin
 *   - Contraseña: admin123
 *
 * Reemplazar por RetrofitClient.apiService cuando haya un backend real.
 */
object MockApiService {

    // Credenciales de prueba
    private const val USUARIO_VALIDO = "admin"
    private const val CONTRASENA_VALIDA = "admin123"

    // Token simulado (JWT ficticio)
    private const val TOKEN_SIMULADO = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyIjoiYWRtaW4iLCJpYXQiOjE3MDAwMDAwMDB9.fake_signature"

    /**
     * Simula POST /auth/login
     * Valida credenciales y devuelve un token JWT simulado.
     */
    suspend fun login(request: LoginRequest): Response<LoginResponse> {
        // Simular latencia de red
        kotlinx.coroutines.delay(1500)

        return if (request.usuario == USUARIO_VALIDO && request.contrasena == CONTRASENA_VALIDA) {
            Response.success(
                LoginResponse(
                    usuario = request.usuario,
                    contrasena = request.contrasena,
                    token = TOKEN_SIMULADO,
                    mensaje = "Autenticación exitosa"
                )
            )
        } else {
            // Simular error: devolver respuesta exitosa pero con token vacío
            Response.success(
                LoginResponse(
                    usuario = "",
                    contrasena = "",
                    token = "",
                    mensaje = "Credenciales incorrectas"
                )
            )
        }
    }

    /**
     * Simula POST /orders
     * Siempre responde éxito si el token es válido.
     */
    suspend fun enviarPedido(token: String, pedido: PedidoRequest): Response<PedidoRequest> {
        // Simular latencia de red
        kotlinx.coroutines.delay(800)

        // Verificar que el token contenga el token simulado válido
        if (!token.contains(TOKEN_SIMULADO)) {
            return Response.error(401, okhttp3.ResponseBody.create(null, "Token inválido"))
        }

        // Devolver el mismo pedido pero con un ID simulado
        return Response.success(
            pedido.copy(id = (100..999).random().toString())
        )
    }

    /**
     * Simula GET /orders
     */
    suspend fun obtenerPedidosPorVendedor(vendedor: String): Response<List<PedidoRequest>> {
        return Response.success(emptyList())
    }
}
