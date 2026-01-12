package com.example.diaaia

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.diaaia.model.DatabaseHelper

class Historial : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial)

        val rvHistorial = findViewById<RecyclerView>(R.id.rvHistorial)
        rvHistorial.layoutManager = LinearLayoutManager(this)

        val dbHelper = DatabaseHelper(this)
        val listaDatos = mutableListOf<RegistroHistorial>()
        val db = dbHelper.readableDatabase

        // 1. Cargar Nutrición
        val cursorC = db.rawQuery("SELECT * FROM nutricion ORDER BY id DESC", null)
        while (cursorC.moveToNext()) {
            val nom = cursorC.getString(cursorC.getColumnIndexOrThrow("alimento"))
            val cal = cursorC.getInt(cursorC.getColumnIndexOrThrow("calorias"))
            listaDatos.add(RegistroHistorial(nom, "$cal kcal", "🍎"))
        }
        cursorC.close()

        // 2. Cargar Entrenamiento con Cálculo
        val cursorE = db.rawQuery("SELECT * FROM entrenamiento ORDER BY id DESC", null)
        while (cursorE.moveToNext()) {
            val ej = cursorE.getString(cursorE.getColumnIndexOrThrow("ejercicio"))
            val s = cursorE.getInt(cursorE.getColumnIndexOrThrow("series"))
            val r = cursorE.getInt(cursorE.getColumnIndexOrThrow("reps"))
            val p = cursorE.getDouble(cursorE.getColumnIndexOrThrow("peso"))

            val volumenTotal = s * r * p
            val textoDetalle = "$s x $r con $p kg (Total: $volumenTotal kg)"

            listaDatos.add(RegistroHistorial(ej, textoDetalle, "🏋️"))
        }
        cursorE.close()

        rvHistorial.adapter = HistorialAdapter(listaDatos)
        findViewById<Button>(R.id.btnVolverHistorial).setOnClickListener { finish() }
    }
}