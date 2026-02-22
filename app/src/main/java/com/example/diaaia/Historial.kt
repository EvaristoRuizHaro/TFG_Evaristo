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
        val listaDatos = mutableListOf<RegistroItem>()
        val db = dbHelper.readableDatabase

        // 1. Cargar Nutrición (Añade el emoji para que el adapter lo reconozca)
        val cursorC = db.rawQuery("SELECT id, alimento, calorias, fecha FROM nutricion ORDER BY id DESC", null)
        while (cursorC.moveToNext()) {
            val id = cursorC.getInt(cursorC.getColumnIndexOrThrow("id"))
            val nom = cursorC.getString(cursorC.getColumnIndexOrThrow("alimento"))
            val cal = cursorC.getInt(cursorC.getColumnIndexOrThrow("calorias"))
            val fecha = cursorC.getString(cursorC.getColumnIndexOrThrow("fecha"))
            listaDatos.add(RegistroItem(id, "🍎 $nom", "$cal kcal", fecha))
        }
        cursorC.close()

        // 2. Cargar Entrenamiento (Añade el emoji para que el adapter lo reconozca)
        val cursorE = db.rawQuery("SELECT id, ejercicio, series, reps, peso, fecha FROM entrenamiento ORDER BY id DESC", null)
        while (cursorE.moveToNext()) {
            val id = cursorE.getInt(cursorE.getColumnIndexOrThrow("id"))
            val ej = cursorE.getString(cursorE.getColumnIndexOrThrow("ejercicio"))
            val s = cursorE.getInt(cursorE.getColumnIndexOrThrow("series"))
            val r = cursorE.getInt(cursorE.getColumnIndexOrThrow("reps"))
            val p = cursorE.getDouble(cursorE.getColumnIndexOrThrow("peso"))
            val fecha = cursorE.getString(cursorE.getColumnIndexOrThrow("fecha"))

            val volumenTotal = s * r * p
            val textoDetalle = "$s x $r con $p kg (Vol: $volumenTotal kg)"
            listaDatos.add(RegistroItem(id, "🏋️ $ej", textoDetalle, fecha))
        }
        cursorE.close()

        // LLAMADA CORREGIDA: Sin el 'true' fijo
        rvHistorial.adapter = HistorialAdapter(listaDatos, dbHelper)

        findViewById<Button>(R.id.btnVolverHistorial).setOnClickListener { finish() }
    }
}