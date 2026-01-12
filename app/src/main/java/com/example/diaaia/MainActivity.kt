package com.example.diaaia

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.diaaia.model.DatabaseHelper

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val dbHelper = DatabaseHelper(this)

        val etUsuario = findViewById<EditText>(R.id.etUsuario)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnIrRegistro = findViewById<Button>(R.id.btnIrARegistro)

        // Botón para INICIAR SESIÓN
        btnLogin.setOnClickListener {
            val userText = etUsuario.text.toString()
            val passText = etPassword.text.toString()

            val db = dbHelper.readableDatabase
            // Consulta SQL parametrizada para evitar inyecciones
            val cursor = db.rawQuery(
                "SELECT * FROM usuarios WHERE nombre = ? AND password = ?",
                arrayOf(userText, passText)
            )

            if (cursor.moveToFirst()) {
                Toast.makeText(this, "¡Bienvenido, $userText!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MenuPrincipal::class.java)
                startActivity(intent)
                finish() // Evita volver al login con el botón atrás
            } else {
                Toast.makeText(this, "Usuario o contraseña incorrectos", Toast.LENGTH_LONG).show()
            }
            cursor.close()
        }

        // Botón para ir a la pantalla de REGISTRO
        btnIrRegistro.setOnClickListener {
            val intent = Intent(this, Registro::class.java)
            startActivity(intent)
        }
    }
}