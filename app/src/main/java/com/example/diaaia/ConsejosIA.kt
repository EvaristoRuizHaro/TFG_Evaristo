package com.example.diaaia

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.diaaia.model.DatabaseHelper
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.launch

class ConsejosIA : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_consejos_ia)

        val tvAnalisis = findViewById<TextView>(R.id.tvAnalisisIA)
        val tvConsejo = findViewById<TextView>(R.id.tvConsejoIA)
        val btnCerrar = findViewById<Button>(R.id.btnCerrarIA)

        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.readableDatabase

        // Obtenemos los datos de la DB
        val cursorNut = db.rawQuery("SELECT SUM(calorias) FROM nutricion WHERE fecha = CURRENT_DATE", null)
        val cal = if (cursorNut.moveToFirst()) cursorNut.getInt(0) else 0
        cursorNut.close()

        val cursorEnt = db.rawQuery("SELECT SUM(series * reps * peso) FROM entrenamiento WHERE fecha = CURRENT_DATE", null)
        val vol = if (cursorEnt.moveToFirst()) cursorEnt.getDouble(0) else 0.0
        cursorEnt.close()

        tvAnalisis.text = "Hoy: $cal kcal consumidas | $vol kg levantados."

        // CONFIGURACIÓN CON TU NUEVA CLAVE
        val generativeModel = GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = "AIzaSyAsvoIKgNprU-LtVHe3LT6o8Mej6hrfH54"
        )

        lifecycleScope.launch {
            try {
                tvConsejo.text = "La IA está pensando..."

                val prompt = "Soy un usuario de gimnasio. Hoy he comido $cal kcal y levantado $vol kg. Dame un consejo de fitness muy breve y motivador."

                val response = generativeModel.generateContent(prompt)
                tvConsejo.text = response.text ?: "La IA no tiene respuesta ahora."

            } catch (e: Exception) {
                // Esto te dirá el error real en la pantalla
                tvConsejo.text = "Error real: ${e.localizedMessage}"
            }
        }

        btnCerrar.setOnClickListener { finish() }
    }
}