package com.example.diaaia

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class Entrenamiento : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entrenamiento)

        // 1. Enlazamos los componentes
        val etEjercicio = findViewById<EditText>(R.id.etEjercicio)
        val etKilos = findViewById<EditText>(R.id.etKilos)
        val etReps = findViewById<EditText>(R.id.etReps)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarSerie)
        val btnVolver = findViewById<Button>(R.id.btnVolverEntrenamiento)

        // 2. Acción para guardar (Lógica para el TFG)
        btnGuardar.setOnClickListener {
            val nombre = etEjercicio.text.toString()
            val kg = etKilos.text.toString()
            val reps = etReps.text.toString()

            if (nombre.isNotEmpty() && kg.isNotEmpty() && reps.isNotEmpty()) {
                Toast.makeText(this, "Serie guardada: $nombre ($kg kg x $reps)", Toast.LENGTH_SHORT).show()
                // Limpiamos solo los campos numéricos para la siguiente serie
                etKilos.text.clear()
                etReps.text.clear()
            } else {
                Toast.makeText(this, "Rellena todos los campos, jefe", Toast.LENGTH_SHORT).show()
            }
        }

        // 3. Acción para volver (Navegación)
        btnVolver.setOnClickListener {
            finish() // Cierra la pantalla y vuelve al menú principal
        }
    }
}