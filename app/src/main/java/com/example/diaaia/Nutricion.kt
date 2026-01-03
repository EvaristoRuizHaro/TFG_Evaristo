package com.example.diaaia

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class Nutricion : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nutricion)

        // 1. Enlazamos todos los componentes
        val etNombre = findViewById<EditText>(R.id.etNombreAlimento)
        val etCalorias = findViewById<EditText>(R.id.etCaloriasAlimento)
        val btnAnadir = findViewById<Button>(R.id.btnAnadirAlimento)
        val btnVolver = findViewById<Button>(R.id.btnVolver) // Enlazado fuera

        // 2. Lógica para añadir alimento
        btnAnadir.setOnClickListener {
            val nombre = etNombre.text.toString()
            val cal = etCalorias.text.toString()

            if (nombre.isNotEmpty() && cal.isNotEmpty()) {
                Toast.makeText(this, "$nombre añadido ($cal kcal)", Toast.LENGTH_SHORT).show()
                etNombre.text.clear()
                etCalorias.text.clear()
            } else {
                Toast.makeText(this, "Por favor, rellena ambos campos", Toast.LENGTH_SHORT).show()
            }
        }

        // 3. Lógica para volver (Independiente del botón añadir)
        btnVolver.setOnClickListener {
            finish() // Cierra esta actividad y vuelve al menú
        }
    }
}