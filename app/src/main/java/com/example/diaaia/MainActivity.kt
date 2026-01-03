
package com.example.diaaia

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Enlazamos con los ID del XML
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val pass = etPassword.text.toString()

            // LOGIN DE PRUEBA (MOCK)
            // Usuario: admin@test.com | Pass: 1234
            if (email == "admin@test.com" && pass == "1234") {
                Toast.makeText(this, "¡Bienvenido, Evaristo!", Toast.LENGTH_SHORT).show()

                // Ir al Menú
                val intent = Intent(this, MenuPrincipal::class.java)
                startActivity(intent)
                finish() // Para que no pueda volver al login con el botón 'atrás'

            } else {
                Toast.makeText(this, "Credenciales incorrectas (Usa: admin@test.com / 1234)", Toast.LENGTH_LONG).show()
            }
        }
    }
}