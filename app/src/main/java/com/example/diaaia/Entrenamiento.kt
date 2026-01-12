package com.example.diaaia

import android.content.ContentValues
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.diaaia.model.DatabaseHelper

class Entrenamiento : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entrenamiento)

        val dbHelper = DatabaseHelper(this)

        val etEjercicio = findViewById<AutoCompleteTextView>(R.id.etEjercicio)
        val etSeries = findViewById<EditText>(R.id.etSeries)
        val etReps = findViewById<EditText>(R.id.etReps)
        val etPeso = findViewById<EditText>(R.id.etPeso)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarEntreno)
        val btnVolver = findViewById<Button>(R.id.btnVolverEntreno)

        // LISTA EXTENSA DE EJERCICIOS
        val listaEjercicios = arrayOf(
            "Press de Banca Plano", "Press de Banca Inclinado", "Press de Banca Declinado", "Aperturas con Mancuernas", "Cruces de Poleas",
            "Sentadilla Libre", "Sentadilla Búlgara", "Prensa de Piernas", "Extensión de Cuádriceps", "Curl Femoral", "Peso Muerto Rumano",
            "Dominadas", "Jalón al Pecho", "Remo con Barra", "Remo en Polea Baja", "Remo con Mancuerna", "Pull-over",
            "Press Militar", "Press Arnold", "Elevaciones Laterales", "Pájaros (Deltoide Posterior)", "Facepull",
            "Curl de Bíceps con Barra", "Curl de Bíceps Martillo", "Curl Predicador",
            "Press Francés", "Extensión de Tríceps en Polea", "Fondos en Paralelas",
            "Zancadas", "Elevación de Talones (Gemelo)", "Hip Thrust", "Plancha Abdominal", "Rueda Abdominal"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, listaEjercicios)
        etEjercicio.setAdapter(adapter)

        btnGuardar.setOnClickListener {
            val nombre = etEjercicio.text.toString()
            val s = etSeries.text.toString().toIntOrNull() ?: 0
            val r = etReps.text.toString().toIntOrNull() ?: 0
            val p = etPeso.text.toString().toDoubleOrNull() ?: 0.0

            if (nombre.isNotEmpty() && s > 0) {
                val db = dbHelper.writableDatabase
                val values = ContentValues().apply {
                    put("ejercicio", nombre)
                    put("series", s)
                    put("reps", r)
                    put("peso", p)
                }
                db.insert("entrenamiento", null, values)
                Toast.makeText(this, "Entrenamiento guardado correctamente", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Por favor, introduce el nombre y las series", Toast.LENGTH_SHORT).show()
            }
        }

        btnVolver.setOnClickListener {
            finish()
        }
    }
}