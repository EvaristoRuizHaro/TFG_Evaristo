package com.example.diaaia

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.diaaia.model.DatabaseHelper
import com.example.diaaia.model.SessionManager
import com.example.diaaia.repository.EntrenamientoRepository
import com.example.diaaia.repository.NutricionRepository
import com.example.diaaia.repository.UsuarioRepository
import com.example.diaaia.ui.SimpleLineChart

/**
 * Pantalla de estadísticas (requisito DESEABLE del TFG).
 *
 * Dibuja con una gráfica propia (sin librerías externas) el progreso de:
 *  - 1RM estimado (fórmula de Epley) del ejercicio seleccionado
 *  - Peso corporal histórico del usuario
 *  - Calorías diarias consumidas
 */
class Estadisticas : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_estadisticas)

        val session = SessionManager(this)
        val dbHelper = DatabaseHelper(this)
        val entrenamientoRepo = EntrenamientoRepository(dbHelper)
        val nutricionRepo = NutricionRepository(dbHelper)
        val usuarioRepo = UsuarioRepository(dbHelper)

        val spEjercicio = findViewById<Spinner>(R.id.spEjercicioEstad)
        val chart1RM = findViewById<SimpleLineChart>(R.id.chart1RM)
        val chartPeso = findViewById<SimpleLineChart>(R.id.chartPesoCorporal)
        val chartKcal = findViewById<SimpleLineChart>(R.id.chartKcal)
        val tvEjVacio = findViewById<TextView>(R.id.tvEjerciciosVacio)
        val btnVolver = findViewById<Button>(R.id.btnVolverEstad)

        // --- 1RM estimado ---
        val ejConHist = entrenamientoRepo.ejerciciosConHistorico(session.usuarioId())
        if (ejConHist.isEmpty()) {
            spEjercicio.visibility = View.GONE
            tvEjVacio.visibility = View.VISIBLE
            chart1RM.setData(emptyList(), "1RM estimado", Color.parseColor("#1565C0"))
        } else {
            tvEjVacio.visibility = View.GONE
            spEjercicio.visibility = View.VISIBLE
            val nombres = ejConHist.map { it.second }
            spEjercicio.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                nombres
            )
            spEjercicio.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    val (ejId, nombre) = ejConHist[pos]
                    val serie = entrenamientoRepo.historico1RM(session.usuarioId(), ejId)
                    chart1RM.setData(serie, "1RM estimado · $nombre (kg)", Color.parseColor("#1565C0"))
                }
                override fun onNothingSelected(p: AdapterView<*>?) {}
            }
        }

        // --- Peso corporal ---
        val pesoHist = usuarioRepo.historicoPeso(session.usuarioId())
        chartPeso.setData(pesoHist, "Peso corporal (kg)", Color.parseColor("#2E7D32"))

        // --- Calorías diarias últimos 30 días ---
        val kcalHist = nutricionRepo.historicoCaloriasDiarias(session.usuarioId(), 30)
        chartKcal.setData(kcalHist, "Calorías diarias (últimos 30 días)", Color.parseColor("#E53935"))

        btnVolver.setOnClickListener { finish() }
    }
}
