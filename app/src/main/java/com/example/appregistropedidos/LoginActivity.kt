package com.example.appregistropedidos

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.appregistropedidos.network.LoginRequest
import com.example.appregistropedidos.network.RetrofitClient
import com.example.appregistropedidos.utils.TokenManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var etUsuario: TextInputEditText
    private lateinit var etContrasena: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var progressLogin: LinearProgressIndicator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Forzar barra de estado blanca con iconos oscuros
        window.statusBarColor = android.graphics.Color.WHITE
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        // Si ya hay sesión activa, ir directo a la lista de pedidos
        if (TokenManager.haySession(this)) {
            irAPedidoList()
            return
        }

        setContentView(R.layout.activity_login)

        etUsuario = findViewById(R.id.etUsuario)
        etContrasena = findViewById(R.id.etContrasena)
        btnLogin = findViewById(R.id.btnLogin)
        progressLogin = findViewById(R.id.progressLogin)

        btnLogin.setOnClickListener { iniciarSesion() }
    }

    private fun iniciarSesion() {
        val usuario = etUsuario.text.toString().trim()
        val contrasena = etContrasena.text.toString().trim()

        if (usuario.isEmpty() || contrasena.isEmpty()) {
            Toast.makeText(this, "Ingrese usuario y contraseña", Toast.LENGTH_SHORT).show()
            return
        }

        // Mostrar progreso y deshabilitar botón
        btnLogin.isEnabled = false
        progressLogin.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.login(usuario, contrasena)
                
                if (response.isSuccessful && response.body() != null) {
                    val usuariosEncontrados = response.body()!!
                    
                    if (usuariosEncontrados.isNotEmpty()) {
                        // VERIFICACIÓN ESTRICTA: MockAPI a veces es flexible con los filtros de búsqueda.
                        // Comprobamos manualmente que el usuario y la contraseña coincidan exactamente.
                        val usuarioValido = usuariosEncontrados.find { 
                            it.usuario == usuario && it.contrasena == contrasena 
                        }

                        if (usuarioValido != null) {
                            TokenManager.guardarSesion(this@LoginActivity, usuarioValido.token, usuarioValido.usuario)
                            Toast.makeText(this@LoginActivity, "✅ Bienvenido ${usuarioValido.usuario}", Toast.LENGTH_SHORT).show()
                            irAPedidoList()
                        } else {
                            Toast.makeText(this@LoginActivity, "❌ Error: Credenciales incorrectas", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this@LoginActivity, "❌ Error: Usuario '$usuario' no existe", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this@LoginActivity, "❌ Error en el servidor", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "❌ Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                btnLogin.isEnabled = true
                progressLogin.visibility = View.GONE
            }
        }
    }

    private fun irAPedidoList() {
        startActivity(Intent(this, PedidoListActivity::class.java))
        finish()
    }
}
