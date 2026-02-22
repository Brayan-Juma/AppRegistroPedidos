package com.example.appregistropedidos

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.appregistropedidos.data.AppDatabase
import com.example.appregistropedidos.utils.TokenManager
import kotlinx.coroutines.launch

class RutaActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var tvTiempoRecorrido: TextView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Forzar barra de estado blanca con iconos oscuros
        window.statusBarColor = android.graphics.Color.WHITE
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        
        setContentView(R.layout.activity_ruta)

        webView = findViewById(R.id.webViewMapa)
        tvTiempoRecorrido = findViewById(R.id.tvTiempoRecorrido)

        // Configurar WebView
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE

        cargarMapa()
    }

    private fun cargarMapa() {
        val db = AppDatabase.getDatabase(this)
        val vendedorActual = TokenManager.obtenerVendedor(this)

        lifecycleScope.launch {
            val pedidos = db.pedidoDao().obtenerPedidosPorVendedor(vendedorActual)

            val pedidosConGPS = pedidos
                .filter { it.latitud != 0.0 || it.longitud != 0.0 }
                .sortedBy { it.fecha }

            if (pedidosConGPS.isEmpty()) {
                tvTiempoRecorrido.text = "Sin datos GPS"
                Toast.makeText(this@RutaActivity, "No hay pedidos con GPS registrado", Toast.LENGTH_LONG).show()

                val htmlDefault = generarHTML(emptyList(), -1.831239, -78.183406, 6)
                webView.loadDataWithBaseURL(null, htmlDefault, "text/html", "UTF-8", null)
                return@launch
            }

            val centerLat = pedidosConGPS.map { it.latitud }.average()
            val centerLng = pedidosConGPS.map { it.longitud }.average()

            val primerFecha = pedidosConGPS.first().fecha
            val ultimaFecha = pedidosConGPS.last().fecha
            val diffMs = ultimaFecha - primerFecha
            val horas = diffMs / (1000 * 60 * 60)
            val minutos = (diffMs / (1000 * 60)) % 60
            val segundos = (diffMs / 1000) % 60

            tvTiempoRecorrido.text = "%02dh %02dm %02ds".format(horas, minutos, segundos)

            val puntos = pedidosConGPS.map {
                MapPoint(it.latitud, it.longitud, it.nombreCliente, it.direccion)
            }

            val html = generarHTML(puntos, centerLat, centerLng, 13)
            webView.loadDataWithBaseURL("https://unpkg.com", html, "text/html", "UTF-8", null)
        }
    }

    data class MapPoint(val lat: Double, val lng: Double, val nombre: String, val direccion: String)

    private fun generarHTML(puntos: List<MapPoint>, centerLat: Double, centerLng: Double, zoom: Int): String {
        val markersJS = StringBuilder()
        val latlngsJS = StringBuilder()

        for ((i, p) in puntos.withIndex()) {
            val safeNombre = p.nombre.replace("'", "\\'").replace("\"", "\\\"")
            val safeDireccion = p.direccion.replace("'", "\\'").replace("\"", "\\\"")
            markersJS.append("L.marker([${p.lat}, ${p.lng}]).addTo(map).bindPopup('<b>${i+1}. $safeNombre</b><br>$safeDireccion');\n")
            latlngsJS.append("[${p.lat}, ${p.lng}],\n")
        }

        val polylineJS = if (puntos.size >= 2) {
            "L.polyline([$latlngsJS], {color: '#1E3A8A', weight: 4, opacity: 0.8, dashArray: '10, 10'}).addTo(map);"
        } else ""

        return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <style>
        body { margin: 0; padding: 0; }
        #map { width: 100%; height: 100vh; }
    </style>
</head>
<body>
    <div id="map"></div>
    <script>
        var map = L.map('map').setView([$centerLat, $centerLng], $zoom);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '© OpenStreetMap',
            maxZoom: 19
        }).addTo(map);
        $markersJS
        $polylineJS
    </script>
</body>
</html>
        """.trimIndent()
    }
}
