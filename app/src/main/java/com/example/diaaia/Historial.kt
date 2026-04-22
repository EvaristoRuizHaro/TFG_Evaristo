package com.example.diaaia

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.diaaia.model.DatabaseHelper
import com.example.diaaia.model.SessionManager
import com.example.diaaia.repository.EntrenamientoRepository
import com.example.diaaia.repository.NutricionRepository

/**
 * Historial combinado de sesiones de entrenamiento y de ingestas del usuario.
 *
 * Lee exclusivamente de las nuevas tablas (sesion_entrenamiento + registro_set
 * y registro_ingesta) respetando el aislamiento multiusuario: solo se muestran
 * los datos del usuario logueado.
 */
class Historial : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial)

        val session = SessionManager(this)
        val dbHelper = DatabaseHelper(this)
        val entrenamientoRepo = EntrenamientoRepository(dbHelper)
        val nutricionRepo = NutricionRepository(dbHelper)

        val rv = findViewById<RecyclerView>(R.id.rvHistorial)
        rv.layoutManager = LinearLayoutManager(this)

        val lista = mutableListOf<RegistroItem>()

        // 1) Sesiones de entrenamiento (con resumen de volumen total)
        for (s in entrenamientoRepo.sesionesUsuario(session.usuarioId())) {
            val sets = entrenamientoRepo.setsDeSesion(s.id)
            val volumen = sets.sumOf { it.peso * it.repsReales }
            val detalle = if (sets.isEmpty()) {
                "Sin sets registrados"
            } else {
                "${sets.size} sets · Vol: ${"%.0f".format(volumen)} kg"
            }
            lista.add(
                RegistroItem(
                    id = s.id,
                    titulo = "🏋️ ${s.nombreRutina}",
                    subtitulo = detalle,
                    fecha = s.fecha
                )
            )
        }

        // 2) Ingestas (todas las del usuario, ordenadas por id DESC)
        val ingestas = cargarTodasLasIngestas(nutricionRepo, session.usuarioId())
        for (i in ingestas) {
            lista.add(
                RegistroItem(
                    id = i.id,
                    titulo = "🍎 ${i.alimentoNombre}",
                    subtitulo = "${"%.0f".format(i.cantidadGramos)} g · ${"%.0f".format(i.calorias)} kcal",
                    fecha = i.fecha
                )
            )
        }

        val tvVacio = findViewById<TextView>(R.id.tvVacioHistorial)
        tvVacio?.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE

        rv.adapter = HistorialAdapter(lista, dbHelper)

        findViewById<Button>(R.id.btnVolverHistorial).setOnClickListener { finish() }
    }

    /**
     * Concatenamos ingestas de los últimos 90 días. Si se necesita más histórico
     * se puede ampliar o convertir en paginación; para el alcance del TFG es
     * suficiente.
     */
    private fun cargarTodasLasIngestas(
        repo: NutricionRepository,
        usuarioId: Int
    ): List<com.example.diaaia.model.RegistroIngesta> {
        val hoy = java.time.LocalDate.now()
        val resultado = mutableListOf<com.example.diaaia.model.RegistroIngesta>()
        for (d in 0..89) {
            val fecha = hoy.minusDays(d.toLong()).toString()
            resultado.addAll(repo.ingestasDeFecha(usuarioId, fecha))
        }
        return resultado
    }
}
