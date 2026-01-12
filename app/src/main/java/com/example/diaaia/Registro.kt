package com.example.diaaia

import android.content.ContentValues
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.diaaia.model.DatabaseHelper

class Registro : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        val dbHelper = DatabaseHelper(this)
        val etUser = findViewById<EditText>(R.id.etNuevoUsuario)
        val etPass = findViewById<EditText>(R.id.etNuevaPassword)
        val btnGuardar = findViewById<Button>(R.id.btnRegistrarGuardar)

        btnGuardar.setOnClickListener {
            val user = etUser.text.toString()
            val pass = etPass.text.toString()

            if (user.isNotEmpty() && pass.isNotEmpty()) {
                val db = dbHelper.writableDatabase
                val values = ContentValues().apply {
                    put("nombre", user)
                    put("password", pass)
                }

                val resultado = db.insert("usuarios", null, values)

                if (resultado != -1L) {
                    Toast.makeText(this, "Usuario creado con éxito", Toast.LENGTH_SHORT).show()
                    finish() // Vuelve a la pantalla de Login
                } else {
                    Toast.makeText(this, "Error al registrar", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Rellena todos los campos", Toast.LENGTH_SHORT).show()
            }
        }
    }
}