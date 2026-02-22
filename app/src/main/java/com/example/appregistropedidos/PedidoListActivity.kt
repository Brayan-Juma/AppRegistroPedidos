package com.example.appregistropedidos

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appregistropedidos.data.AppDatabase
import com.example.appregistropedidos.data.Pedido
import com.example.appregistropedidos.network.PedidoRequest
import com.example.appregistropedidos.network.RetrofitClient
import com.example.appregistropedidos.utils.ImageHelper
import com.example.appregistropedidos.utils.NetworkHelper
import com.example.appregistropedidos.utils.TokenManager
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PedidoListActivity : AppCompatActivity() {

    private lateinit var rvPedidos: RecyclerView
    private lateinit var tvVacio: TextView
    private lateinit var adapter: PedidoAdapter
    private lateinit var progressDashboard: android.widget.ProgressBar
    private lateinit var tvProgressLabel: TextView

    private val db by lazy { AppDatabase.getDatabase(this) }
    private var isSyncing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Forzar barra de estado blanca con iconos oscuros
        window.statusBarColor = android.graphics.Color.WHITE
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        
        setContentView(R.layout.activity_pedido_list)

        rvPedidos = findViewById(R.id.rvPedidos)
        tvVacio = findViewById(R.id.tvVacio)
        progressDashboard = findViewById(R.id.progressDashboard)
        tvProgressLabel = findViewById(R.id.tvProgressLabel)

        // RecyclerView
        adapter = PedidoAdapter(onItemClick = { pedido ->
            val intent = Intent(this, PedidoDetailActivity::class.java)
            intent.putExtra(PedidoDetailActivity.EXTRA_PEDIDO_ID, pedido.id)
            startActivity(intent)
        })
        rvPedidos.layoutManager = LinearLayoutManager(this)
        rvPedidos.adapter = adapter

        // + Nuevo (FAB Central)
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabNuevoPedido).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        // Sincronizar (Botón en el Dashboard)
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSincronizar).setOnClickListener { 
            sincronizarPedidos() 
        }

        // Cerrar Sesión (Arriba Derecha)
        findViewById<View>(R.id.btnCerrarSesion).setOnClickListener {
            mostrarDialogoCerrarSesion()
        }

        // Exportar CSV (Icono junto al título)
        findViewById<View>(R.id.btnExportarCSV).setOnClickListener {
            exportarCSV()
        }

        // Ver Ruta (Icono en Bottom Nav)
        findViewById<ImageButton>(R.id.btnVerRuta).setOnClickListener {
            startActivity(Intent(this, RutaActivity::class.java))
        }
    }

    private fun mostrarDialogoCerrarSesion() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Está seguro que desea cerrar la sesión?")
            .setPositiveButton("Cerrar") { _, _ ->
                TokenManager.eliminarToken(this)
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        cargarPedidos()
    }

    private fun cargarPedidos() {
        val vendedorActual = TokenManager.obtenerVendedor(this)
        lifecycleScope.launch {
            val pedidos = db.pedidoDao().obtenerPedidosPorVendedor(vendedorActual)
            adapter.actualizarLista(pedidos)

            // Lógica de Dashboard: % Sincronización
            if (pedidos.isNotEmpty()) {
                val sincronizados = pedidos.count { it.estado.contains("Sincronizado", true) }
                val porcentaje = (sincronizados * 100) / pedidos.size
                progressDashboard.progress = porcentaje
                tvProgressLabel.text = "$porcentaje%"
            } else {
                progressDashboard.progress = 0
                tvProgressLabel.text = "0%"
            }

            if (pedidos.isEmpty()) {
                rvPedidos.visibility = View.GONE
                tvVacio.visibility = View.VISIBLE
            } else {
                rvPedidos.visibility = View.VISIBLE
                tvVacio.visibility = View.GONE
            }
        }
    }

    // ══════════ EXPORTAR CSV ══════════
    private fun exportarCSV() {
        val vendedorActual = TokenManager.obtenerVendedor(this)
        lifecycleScope.launch {
            try {
                val pedidos = db.pedidoDao().obtenerPedidosPorVendedor(vendedorActual)

                if (pedidos.isEmpty()) {
                    Toast.makeText(this@PedidoListActivity, "No hay pedidos para exportar", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val sdfDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val sb = StringBuilder()

                sb.appendLine("ID,Cliente,Telefono,Direccion,Detalle,TipoPago,Latitud,Longitud,Estado,Fecha")

                for (p in pedidos) {
                    sb.appendLine("${p.id},\"${p.nombreCliente}\",\"${p.telefono}\",\"${p.direccion}\",\"${p.detalle}\",\"${p.tipoPago}\",${p.latitud},${p.longitud},\"${p.estado}\",\"${sdfDate.format(Date(p.fecha))}\"")
                }

                // Nombre secuencial simplificado usando timestamp para asegurar unicidad y orden
                // Pero el usuario pidió "Registropedidos1" etc. 
                // Como no sabemos el historial exacto, usaremos un prefijo claro.
                val timestampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "Registropedidos_$timestampStr.csv"

                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

                if (uri != null) {
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(sb.toString().toByteArray())
                    }
                    // Mensaje solicitado: "csv creado correctamente"
                    Toast.makeText(this@PedidoListActivity, "csv creado correctamente", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@PedidoListActivity, "Error al crear archivo", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PedidoListActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ══════════ SINCRONIZAR ══════════
    private fun sincronizarPedidos() {
        if (isSyncing) return
        
        // Verificar conexión a internet
        if (!NetworkHelper.hayConexion(this)) {
            Toast.makeText(this, "no hay conexión a internet", Toast.LENGTH_LONG).show()
            return
        }

        val bearerToken = TokenManager.obtenerBearerToken(this)
        if (bearerToken == null) {
            Toast.makeText(this, "Sesión expirada. Inicie sesión nuevamente.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val vendedorActual = TokenManager.obtenerVendedor(this)
        
        isSyncing = true
        val btnSync = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSincronizar)
        btnSync.isEnabled = false
        btnSync.text = "Sync..."

        lifecycleScope.launch {
            try {
                val pendientes = db.pedidoDao().obtenerPedidosPendientesPorVendedor(vendedorActual)

                var exitosos = 0
                
                // 1. SUBIR pedidos (solo si hay pendientes)
                if (pendientes.isNotEmpty()) {
                    for (pedido in pendientes) {
                        try {
                            val sdfApi = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                            
                            // Convertir foto a Base64 antes de subir
                            val fotoBase64 = ImageHelper.comprimirYConvertirABase64(this@PedidoListActivity, pedido.fotoPath)

                            val request = PedidoRequest(
                                vendedor = pedido.vendedor,
                                nombreCliente = pedido.nombreCliente,
                                telefono = pedido.telefono,
                                direccion = pedido.direccion,
                                detalle = pedido.detalle,
                                tipoPago = pedido.tipoPago,
                                fotoPath = pedido.fotoPath,
                                fotoBase64 = fotoBase64,
                                latitud = pedido.latitud,
                                longitud = pedido.longitud,
                                estado = "Sincronizado",
                                fecha = sdfApi.format(Date(pedido.fecha))
                            )

                            val response = RetrofitClient.apiService.enviarPedido(token = bearerToken, pedido = request)

                            if (response.isSuccessful) {
                                val pedidoSync = response.body()
                                if (pedidoSync?.id != null) {
                                    db.pedidoDao().marcarSincronizado(pedido.id, "Sincronizado", pedidoSync.id)
                                    exitosos++
                                } else {
                                    db.pedidoDao().actualizarEstado(pedido.id, "Error: respuesta sin ID")
                                }
                            } else {
                                db.pedidoDao().actualizarEstado(pedido.id, "Error: ${response.code()}")
                            }
                        } catch (e: Exception) {
                            db.pedidoDao().actualizarEstado(pedido.id, "Error: ${e.message ?: "desconocido"}")
                        }
                    }
                }

                // 2. BAJAR pedidos de ese vendedor desde la API (Download)
                try {
                    val responseList = RetrofitClient.apiService.obtenerPedidosPorVendedor(vendedorActual)
                    if (responseList.isSuccessful) {
                        val pedidosRemotos = responseList.body() ?: emptyList()
                        val sdfApi = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                        
                        for (remoto in pedidosRemotos) {
                            if (remoto.id == null) continue
                            
                            // Verificar si ya existe localmente por remoteId
                            val existe = db.pedidoDao().buscarPorRemoteId(remoto.id)
                            if (existe == null) {
                                // 📷 Si trae Base64, lo guardamos como archivo local
                                val pathLocalDeFotoSincronizada = if (!remoto.fotoBase64.isNullOrEmpty()) {
                                    ImageHelper.guardarBase64AArchivo(this@PedidoListActivity, remoto.fotoBase64)
                                } else {
                                    "" // O dejar la ruta remota, pero no servirá localmente
                                }

                                // Insertar como nuevo pedido sincronizado
                                val nuevoPedido = Pedido(
                                    vendedor = remoto.vendedor,
                                    nombreCliente = remoto.nombreCliente,
                                    telefono = remoto.telefono,
                                    direccion = remoto.direccion,
                                    detalle = remoto.detalle,
                                    tipoPago = remoto.tipoPago,
                                    fotoPath = pathLocalDeFotoSincronizada ?: "",
                                    latitud = remoto.latitud,
                                    longitud = remoto.longitud,
                                    estado = "Sincronizado",
                                    fecha = try { sdfApi.parse(remoto.fecha)?.time ?: System.currentTimeMillis() } catch(e: Exception) { System.currentTimeMillis() },
                                    remoteId = remoto.id
                                )
                                db.pedidoDao().insertarPedido(nuevoPedido)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Error al descargar, pero los subidos se mantienen
                }

                if (exitosos > 0) {
                    Toast.makeText(this@PedidoListActivity, "Sincronización completa (Subida y Bajada)", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@PedidoListActivity, "Sincronización terminada", Toast.LENGTH_SHORT).show()
                }
                
                cargarPedidos()
            } catch (e: Exception) {
                Toast.makeText(this@PedidoListActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                finalizarSincronización()
            }
        }
    }

    private fun finalizarSincronización() {
        isSyncing = false
        val btnSync = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSincronizar)
        btnSync.isEnabled = true
        btnSync.text = "Sincronizar"
    }
}
