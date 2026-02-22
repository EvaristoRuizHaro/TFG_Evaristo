package com.example.diaaia

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.diaaia.model.DatabaseHelper
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConsejosIA : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_consejos_ia)

        val tvAnalisis = findViewById<TextView>(R.id.tvAnalisisIA)
        val tvConsejo = findViewById<TextView>(R.id.tvConsejoIA)
        val btnCerrar = findViewById<Button>(R.id.btnCerrarIA)

        val dbHelper = DatabaseHelper(this)

        // 1. Obtener datos en un hilo secundario para no bloquear la pantalla
        lifecycleScope.launch {
            val (cal, vol) = withContext(Dispatchers.IO) {
                val db = dbHelper.readableDatabase

                val cursorNut = db.rawQuery("SELECT SUM(calorias) FROM nutricion WHERE fecha = CURRENT_DATE", null)
                val c = if (cursorNut.moveToFirst()) cursorNut.getInt(0) else 0
                cursorNut.close()

                val cursorEnt = db.rawQuery("SELECT SUM(series * reps * peso) FROM entrenamiento WHERE fecha = CURRENT_DATE", null)
                val v = if (cursorEnt.moveToFirst()) cursorEnt.getDouble(0) else 0.0
                cursorEnt.close()

                Pair(c, v)
            }

            tvAnalisis.text = "Hoy: $cal kcal consumidas | $vol kg levantados."

            // 2. Llamada a la IA
            llamarIA(cal, vol, tvConsejo)
        }

        btnCerrar.setOnClickListener { finish() }
    }

    private fun llamarIA(cal: Int, vol: Double, tvConsejo: TextView) {
        // RECOMENDACIÓN: Si sigue sin funcionar, genera una API KEY nueva en Google AI Studio
        val generativeModel = GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = "AIzaSyBejo2sFK_lHjkUXzC3ATVuQQi4O2fhpyE"
        )

        lifecycleScope.launch {
            try {
                tvConsejo.text = "La IA está analizando tus datos..."

                val prompt = "Resumen: $cal kcal y $vol kg. Dame un consejo de fitness de 2 frases, motivador y directo."

                val response = generativeModel.generateContent(prompt)
                tvConsejo.text = response.text ?: "No hay consejos disponibles por ahora."

            } catch (e: Exception) {
                // Si sale "API_KEY_INVALID", debes generar una nueva
                tvConsejo.text = "Aviso: Verifica tu conexión o API Key."
                e.printStackTrace()
            }
        }
    }
}