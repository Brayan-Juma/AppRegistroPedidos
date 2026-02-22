package com.example.appregistropedidos

import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.appregistropedidos.data.AppDatabase
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PedidoDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PEDIDO_ID = "pedido_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Forzar barra de estado integrada con el fondo suave
        window.statusBarColor = ContextCompat.getColor(this, R.color.app_background)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        
        setContentView(R.layout.activity_pedido_detail)

        val pedidoId = intent.getIntExtra(EXTRA_PEDIDO_ID, -1)
        if (pedidoId == -1) {
            finish()
            return
        }

        val db = AppDatabase.getDatabase(this)

        lifecycleScope.launch {
            val pedido = db.pedidoDao().obtenerTodosLosPedidos().find { it.id == pedidoId }
            if (pedido == null) {
                finish()
                return@launch
            }

            // Datos del cliente
            findViewById<TextView>(R.id.tvDetalleNombre).text = "👤 ${pedido.nombreCliente}"
            findViewById<TextView>(R.id.tvDetalleTelefono).text = "📞 ${pedido.telefono}"
            findViewById<TextView>(R.id.tvDetalleDireccion).text = "📍 ${pedido.direccion}"

            // Detalle del pedido
            findViewById<TextView>(R.id.tvDetalleDetalle).text = pedido.detalle
            findViewById<TextView>(R.id.tvDetalleTipoPago).text = "💳 ${pedido.tipoPago}"

            // Estado con badge pill
            val tvEstado = findViewById<TextView>(R.id.tvDetalleEstado)
            when {
                pedido.estado.contains("Sincronizado", ignoreCase = true) -> {
                    tvEstado.text = "✓ Sincronizado"
                    tvEstado.setTextColor(ContextCompat.getColor(this@PedidoDetailActivity, R.color.emerald_600))
                    tvEstado.background = null
                }
                pedido.estado.contains("Error", ignoreCase = true) -> {
                    tvEstado.text = "✕ Error"
                    tvEstado.setTextColor(ContextCompat.getColor(this@PedidoDetailActivity, R.color.rose_600))
                    tvEstado.background = null
                }
                else -> {
                    tvEstado.text = "• Pendiente"
                    tvEstado.setTextColor(ContextCompat.getColor(this@PedidoDetailActivity, R.color.amber_600))
                    tvEstado.background = null
                }
            }

            // Fotografía
            val ivFoto = findViewById<ImageView>(R.id.ivDetalleFoto)
            val tvSinFoto = findViewById<TextView>(R.id.tvSinFoto)

            if (pedido.fotoPath.isNotEmpty() && File(pedido.fotoPath).exists()) {
                val bitmap = BitmapFactory.decodeFile(pedido.fotoPath)
                ivFoto.setImageBitmap(bitmap)
                ivFoto.visibility = View.VISIBLE
                tvSinFoto.visibility = View.GONE
            } else {
                ivFoto.visibility = View.GONE
                tvSinFoto.visibility = View.VISIBLE
            }

            // GPS
            findViewById<TextView>(R.id.tvDetalleGPS).text =
                "Lat: ${pedido.latitud}  |  Lng: ${pedido.longitud}"

            // Fecha
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            findViewById<TextView>(R.id.tvDetalleFecha).text = "🕐 ${sdf.format(Date(pedido.fecha))}"
        }
    }
}
