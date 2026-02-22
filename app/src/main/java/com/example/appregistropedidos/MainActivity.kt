package com.example.appregistropedidos

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.appregistropedidos.data.AppDatabase
import com.example.appregistropedidos.data.Pedido
import com.example.appregistropedidos.utils.LocationHelper
import com.example.appregistropedidos.utils.TokenManager
import com.google.android.material.button.MaterialButton
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    // ══════════ VISTAS ══════════
    private lateinit var etNombreCliente: EditText
    private lateinit var etTelefono: EditText
    private lateinit var etDireccion: EditText
    private lateinit var etDetallePedido: EditText
    private lateinit var rgTipoPago: RadioGroup
    private lateinit var rbEfectivo: RadioButton
    private lateinit var rbTransferencia: RadioButton
    private lateinit var ivFotoPreview: ImageView
    private lateinit var btnLeerQR: LinearLayout
    private lateinit var btnTomarFoto: LinearLayout
    private lateinit var btnGuardar: MaterialButton

    // ══════════ DATOS ══════════
    private var fotoPath: String = ""
    private var fotoUri: Uri? = null
    private val db by lazy { AppDatabase.getDatabase(this) }

    // GPS en tiempo real
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var currentLat: Double = 0.0
    private var currentLng: Double = 0.0

    // ══════════ PERMISOS ══════════
    private val permisosRequeridos = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val solicitarPermisosLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permisos ->
            val todosOtorgados = permisos.all { it.value }
            if (todosOtorgados) {
                Toast.makeText(this, "Permisos otorgados correctamente", Toast.LENGTH_SHORT).show()
                instalarModuloScanner()
            } else {
                mostrarDialogoPermisosObligatorios()
            }
        }

    // ══════════ LAUNCHER: CÁMARA ══════════
    private val tomarFotoLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { exito ->
            if (exito) {
                ivFotoPreview.visibility = View.VISIBLE
                val bitmap = BitmapFactory.decodeFile(fotoPath)
                ivFotoPreview.setImageBitmap(bitmap)

                // Cambiar estilo foto card a "guardada"
                btnTomarFoto.setBackgroundResource(R.drawable.action_card_photo)
                val ivIcon = findViewById<ImageView>(R.id.ivFotoIcon)
                val tvLabel = findViewById<TextView>(R.id.tvFotoLabel)
                ivIcon?.setColorFilter(ContextCompat.getColor(this, R.color.emerald_600))
                tvLabel?.text = "GUARDADA"
                tvLabel?.setTextColor(ContextCompat.getColor(this, R.color.emerald_600))

                Toast.makeText(this, "Foto capturada correctamente", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No se capturó la foto", Toast.LENGTH_SHORT).show()
            }
        }

    // ══════════ LIFECYCLE ══════════
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Forzar barra de estado integrada con el fondo suave
        window.statusBarColor = ContextCompat.getColor(this, R.color.app_background)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        
        setContentView(R.layout.activity_main)

        inicializarVistas()
        configurarBotones()
        verificarPermisos()
        instalarModuloScanner()
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        prepararCallbackGPS()
    }

    override fun onStart() {
        super.onStart()
        iniciarRastreoGPS()
    }

    override fun onStop() {
        super.onStop()
        detenerRastreoGPS()
    }

    private fun prepararCallbackGPS() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let {
                    currentLat = it.latitude
                    currentLng = it.longitude
                }
            }
        }
    }

    private fun iniciarRastreoGPS() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            == PackageManager.PERMISSION_GRANTED) {
            
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .build()

            locationCallback?.let {
                fusedLocationClient.requestLocationUpdates(locationRequest, it, mainLooper)
            }
        }
    }

    private fun detenerRastreoGPS() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
    }

    // ══════════ INICIALIZACIÓN ══════════
    private fun inicializarVistas() {
        etNombreCliente = findViewById(R.id.etNombreCliente)
        etTelefono = findViewById(R.id.etTelefono)
        etDireccion = findViewById(R.id.etDireccion)
        etDetallePedido = findViewById(R.id.etDetallePedido)
        rgTipoPago = findViewById(R.id.rgTipoPago)
        rbEfectivo = findViewById(R.id.rbEfectivo)
        rbTransferencia = findViewById(R.id.rbTransferencia)
        ivFotoPreview = findViewById(R.id.ivFotoPreview)
        btnLeerQR = findViewById(R.id.btnLeerQR)
        btnTomarFoto = findViewById(R.id.btnTomarFoto)
        btnGuardar = findViewById(R.id.btnGuardar)
    }

    private fun configurarBotones() {
        // Back button
        findViewById<ImageButton>(R.id.btnVolver).setOnClickListener { finish() }

        // QR - now uses live Google Code Scanner
        btnLeerQR.setOnClickListener { lanzarEscaneoQR() }

        // Foto
        btnTomarFoto.setOnClickListener { lanzarCapturaDeFoto() }
        // Animación pulse en el botón de foto
        val pulseAnim = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.pulse_photo)
        btnTomarFoto.startAnimation(pulseAnim)

        // Guardar
        btnGuardar.setOnClickListener { guardarPedido() }

        // Toggle de pago: cambiar backgrounds
        rgTipoPago.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbEfectivo -> {
                    rbEfectivo.setBackgroundResource(R.drawable.toggle_selected_bg)
                    rbEfectivo.setTextColor(ContextCompat.getColor(this, R.color.primary))
                    rbTransferencia.setBackgroundResource(R.drawable.toggle_unselected_bg)
                    rbTransferencia.setTextColor(ContextCompat.getColor(this, R.color.slate_400))
                }
                R.id.rbTransferencia -> {
                    rbTransferencia.setBackgroundResource(R.drawable.toggle_selected_bg)
                    rbTransferencia.setTextColor(ContextCompat.getColor(this, R.color.primary))
                    rbEfectivo.setBackgroundResource(R.drawable.toggle_unselected_bg)
                    rbEfectivo.setTextColor(ContextCompat.getColor(this, R.color.slate_400))
                }
            }
        }
    }

    /**
     * Instala el módulo del escáner de Google si no está disponible.
     */
    private fun instalarModuloScanner() {
        val moduleInstall = ModuleInstall.getClient(this)
        val optionalModuleApi = GmsBarcodeScanning.getClient(this)
        moduleInstall
            .areModulesAvailable(optionalModuleApi)
            .addOnSuccessListener {
                if (!it.areModulesAvailable()) {
                    // Instalar módulo en background
                    val request = ModuleInstallRequest.newBuilder()
                        .addApi(optionalModuleApi)
                        .build()
                    moduleInstall.installModules(request)
                }
            }
    }

    // ══════════ PERMISOS ══════════
    private fun verificarPermisos() {
        val permisosFaltantes = permisosRequeridos.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (permisosFaltantes.isNotEmpty()) {
            solicitarPermisosLauncher.launch(permisosFaltantes.toTypedArray())
        }
    }

    private fun mostrarDialogoPermisosObligatorios() {
        AlertDialog.Builder(this)
            .setTitle("¡Permisos Obligatorios!")
            .setMessage(
                "Para poder registrar pedidos, la aplicación NECESITA obligatoriamente:\n\n" +
                "1. Cámara: Para la foto del pedido.\n" +
                "2. Ubicación: Para saber dónde se realizó.\n\n" +
                "Si no otorgas estos permisos, no podrás usar la aplicación."
            )
            .setPositiveButton("Configurar en Ajustes") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cerrar App") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    // ══════════ QR: LIVE SCANNER CON GOOGLE CODE SCANNER ══════════
    /**
     * Lanza el escáner QR con visor en vivo.
     * Google Code Scanner detecta automáticamente el código sin necesidad de tomar foto.
     */
    private fun lanzarEscaneoQR() {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_ALL_FORMATS
            )
            .enableAutoZoom()
            .build()

        val scanner = GmsBarcodeScanning.getClient(this, options)

        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val rawValue = barcode.rawValue ?: ""
                if (rawValue.isNotEmpty()) {
                    parsearDatosQR(rawValue)
                    Toast.makeText(this, "✅ QR leído: datos completados", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "QR vacío", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnCanceledListener {
                Toast.makeText(this, "Escaneo QR cancelado", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al escanear: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    /**
     * Parsea el string del QR con formato:
     * "CLIENTE=Juan Perez|TEL=0999999999|DIR=Av. Central y Loja"
     */
    private fun parsearDatosQR(raw: String) {
        try {
            val partes = raw.split("|")
            val datos = mutableMapOf<String, String>()

            for (parte in partes) {
                val separador = parte.indexOf("=")
                if (separador > 0) {
                    val clave = parte.substring(0, separador).trim().uppercase()
                    val valor = parte.substring(separador + 1).trim()
                    datos[clave] = valor
                }
            }

            datos["CLIENTE"]?.let { etNombreCliente.setText(it) }
            datos["TEL"]?.let { etTelefono.setText(it) }
            datos["DIR"]?.let { etDireccion.setText(it) }

            if (datos.isEmpty()) {
                etNombreCliente.setText(raw)
                Toast.makeText(this, "Formato QR no reconocido, datos colocados en Nombre", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            etNombreCliente.setText(raw)
            Toast.makeText(this, "Formato QR no reconocido, datos colocados en Nombre", Toast.LENGTH_LONG).show()
        }
    }

    // ══════════ CÁMARA: CAPTURA DE FOTO ══════════
    private fun lanzarCapturaDeFoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Se requiere permiso de cámara", Toast.LENGTH_SHORT).show()
            verificarPermisos()
            return
        }

        val archivoFoto = crearArchivoFoto("pedido")
        fotoPath = archivoFoto.absolutePath
        fotoUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", archivoFoto)
        tomarFotoLauncher.launch(fotoUri!!)
    }

    private fun crearArchivoFoto(prefijo: String): File {
        val directorio = File(filesDir, "fotos")
        if (!directorio.exists()) directorio.mkdirs()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(directorio, "${prefijo}_${timestamp}.jpg")
    }

    // ══════════ GUARDAR PEDIDO ══════════
    private fun guardarPedido() {
        val nombre = etNombreCliente.text.toString().trim()
        val telefono = etTelefono.text.toString().trim()
        val direccion = etDireccion.text.toString().trim()
        val detalle = etDetallePedido.text.toString().trim()

        if (nombre.isEmpty() || telefono.isEmpty() || direccion.isEmpty() || detalle.isEmpty()) {
            Toast.makeText(this, "Por favor, complete todos los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        val tipoPago = when (rgTipoPago.checkedRadioButtonId) {
            R.id.rbEfectivo -> "Efectivo"
            R.id.rbTransferencia -> "Transferencia"
            else -> "Efectivo"
        }

        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val gpsActivado = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (!gpsActivado) {
            mostrarDialogoActivarGPS()
            return
        }

        // Si ya tenemos coordenadas capturadas en tiempo real, guardar inmediatamente
        if (currentLat != 0.0 || currentLng != 0.0) {
            ejecutarGuardadoFinal(nombre, telefono, direccion, detalle, tipoPago, currentLat, currentLng)
        } else {
            // Caso raro: aún no se ha obtenido el primer fix, intentamos una vez más rápido
            btnGuardar.isEnabled = false
            btnGuardar.text = "Ubicando..."
            
            LocationHelper.obtenerUbicacionActual(this) { lat, lng ->
                currentLat = lat
                currentLng = lng
                ejecutarGuardadoFinal(nombre, telefono, direccion, detalle, tipoPago, lat, lng)
            }
        }
    }

    private fun ejecutarGuardadoFinal(
        nombre: String, telefono: String, direccion: String,
        detalle: String, tipoPago: String, latitud: Double, longitud: Double
    ) {
        val pedido = Pedido(
            vendedor = TokenManager.obtenerVendedor(this),
            nombreCliente = nombre,
            telefono = telefono,
            direccion = direccion,
            detalle = detalle,
            tipoPago = tipoPago,
            fotoPath = fotoPath,
            latitud = latitud,
            longitud = longitud,
            estado = "Pendiente",
            fecha = System.currentTimeMillis()
        )

        lifecycleScope.launch {
            try {
                db.pedidoDao().insertarPedido(pedido)
                Toast.makeText(this@MainActivity, "✅ Pedido guardado exitosamente", Toast.LENGTH_LONG).show()
                limpiarFormulario()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                btnGuardar.isEnabled = true
                btnGuardar.text = getString(R.string.btn_guardar)
            }
        }
    }

    private fun mostrarDialogoActivarGPS() {
        AlertDialog.Builder(this)
            .setTitle("GPS desactivado")
            .setMessage(
                "El GPS está desactivado. Es necesario activarlo para " +
                "registrar la ubicación del pedido."
            )
            .setPositiveButton("Activar GPS") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Cancelar", null)
            .setCancelable(false)
            .show()
    }

    private fun limpiarFormulario() {
        etNombreCliente.text.clear()
        etTelefono.text.clear()
        etDireccion.text.clear()
        etDetallePedido.text.clear()
        rgTipoPago.check(R.id.rbEfectivo)
        ivFotoPreview.visibility = View.GONE
        ivFotoPreview.setImageDrawable(null)
        fotoPath = ""
        fotoUri = null

        // Reset foto card
        btnTomarFoto.setBackgroundResource(R.drawable.action_card_default)
        val ivIcon = findViewById<ImageView>(R.id.ivFotoIcon)
        val tvLabel = findViewById<TextView>(R.id.tvFotoLabel)
        ivIcon?.setColorFilter(ContextCompat.getColor(this, R.color.slate_400))
        tvLabel?.text = "CAPTURAR FOTOGRAFÍA"
        tvLabel?.setTextColor(ContextCompat.getColor(this, R.color.slate_400))
    }
}