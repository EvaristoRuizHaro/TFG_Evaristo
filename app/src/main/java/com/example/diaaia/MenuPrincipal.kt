package com.example.diaaia

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MenuPrincipal : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_principal)

        // 1. Enlazamos los botones del XML
        val btnEntrenamiento = findViewById<Button>(R.id.btnEntrenamiento)
        val btnDieta = findViewById<Button>(R.id.btnDieta)
        val btnIA = findViewById<Button>(R.id.btnProgresoIA)

        // 2. Conectamos el botón de ENTRENAMIENTO
        btnEntrenamiento.setOnClickListener {
            val intent = Intent(this, Entrenamiento::class.java)
            startActivity(intent)
        }

        // 3. Conectamos el botón de DIETA (Nutrición)
        btnDieta.setOnClickListener {
            val intent = Intent(this, Nutricion::class.java)
            startActivity(intent)
        }

        // 4. Botón de consejos (lo dejaremos para más adelante)
        btnIA.setOnClickListener {
            // Aquí irá la lógica de la IA
        }
    }
}