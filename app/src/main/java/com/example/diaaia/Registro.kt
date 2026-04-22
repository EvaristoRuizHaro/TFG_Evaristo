package com.example.diaaia

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.diaaia.model.DatabaseHelper
import com.example.diaaia.repository.UsuarioRepository

/**
 * Pantalla de Registro de nuevo usuario.
 *
 * Permite crear cuentas con rol "cliente" (por defecto) o "entrenador" (marcando
 * el checkbox). La contraseña se almacena hasheada por [UsuarioRepository].
 */
class Registro : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        val dbHelper = DatabaseHelper(this)
        val usuarioRepo = UsuarioRepository(dbHelper)

        val etUser = findViewById<EditText>(R.id.etNuevoUsuario)
        val etPass = findViewById<EditText>(R.id.etNuevaPassword)
        val cbEntrenador = findViewById<CheckBox>(R.id.cbEntrenador)
        val btnGuardar = findViewById<Button>(R.id.btnRegistrarGuardar)

        btnGuardar.setOnClickListener {
            val user = etUser.text.toString().trim()
            val pass = etPass.text.toString()

            if (user.length < 3) {
                etUser.error = "Mínimo 3 caracteres"
                return@setOnClickListener
            }
            if (pass.length < 4) {
                etPass.error = "Mínimo 4 caracteres"
                return@setOnClickListener
            }

            val rol = if (cbEntrenador.isChecked) "entrenador" else "cliente"
            val id = usuarioRepo.registrar(user, pass, rol)

            if (id > 0) {
                Toast.makeText(this, "Usuario creado con éxito. Inicia sesión.", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(
                    this,
                    "Ese nombre de usuario ya existe o hubo un error al registrar.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
