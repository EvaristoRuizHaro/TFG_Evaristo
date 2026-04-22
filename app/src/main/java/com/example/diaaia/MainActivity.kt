package com.example.diaaia

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.diaaia.model.DatabaseHelper
import com.example.diaaia.model.SessionManager
import com.example.diaaia.repository.UsuarioRepository

/**
 * Pantalla de Login.
 *
 * Usa [UsuarioRepository] para autenticar (con contraseña hasheada) y [SessionManager]
 * para persistir la sesión entre reinicios de la app.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sessionManager = SessionManager(this)

        // Autologin: si ya hay sesión guardada, saltamos directo al menú
        if (sessionManager.estaLogueado()) {
            startActivity(Intent(this, MenuPrincipal::class.java))
            finish()
            return
        }

        val dbHelper = DatabaseHelper(this)
        val usuarioRepo = UsuarioRepository(dbHelper)

        val etUsuario = findViewById<EditText>(R.id.etUsuario)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnIrRegistro = findViewById<Button>(R.id.btnIrARegistro)

        btnLogin.setOnClickListener {
            val user = etUsuario.text.toString().trim()
            val pass = etPassword.text.toString()

            if (user.isEmpty() || pass.isEmpty()) {
                etUsuario.error = if (user.isEmpty()) "Introduce tu usuario" else null
                etPassword.error = if (pass.isEmpty()) "Introduce tu contraseña" else null
                return@setOnClickListener
            }

            val usuario = usuarioRepo.autenticar(user, pass)
            if (usuario != null) {
                sessionManager.guardarSesion(usuario.id, usuario.nombre, usuario.rol)
                Toast.makeText(this, "¡Bienvenido, ${usuario.nombre}!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MenuPrincipal::class.java))
                finish()
            } else {
                Toast.makeText(this, "Usuario o contraseña incorrectos", Toast.LENGTH_LONG).show()
            }
        }

        btnIrRegistro.setOnClickListener {
            startActivity(Intent(this, Registro::class.java))
        }
    }
}
