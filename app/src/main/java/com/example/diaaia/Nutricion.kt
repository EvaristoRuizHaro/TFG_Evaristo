package com.example.diaaia

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.diaaia.model.DatabaseHelper
import com.example.diaaia.model.RegistroIngesta
import com.example.diaaia.model.SessionManager
import com.example.diaaia.repository.NutricionRepository
import com.example.diaaia.repository.UsuarioRepository

/**
 * Pantalla de Nutrición. Cumple el requisito MVP "Registro Nutricional":
 *  - Muestra los 4 macros del día (kcal, proteínas, carbohidratos, grasas) frente
 *    al objetivo del usuario, con barras de progreso.
 *  - Permite añadir ingestas seleccionando un alimento del catálogo y la cantidad
 *    consumida en gramos.
 *  - Lista las ingestas del día con desglose de macros escalado por gramaje.
 *  - Accede al asistente IA nutricional (ConsejosIA).
 */
class Nutricion : AppCompatActivity() {

    private lateinit var nutricionRepo: NutricionRepository
    private lateinit var usuarioRepo: UsuarioRepository
    private lateinit var session: SessionManager

    private val ingestasHoy = mutableListOf<RegistroIngesta>()
    private lateinit var adapter: IngestaAdapter

    private var alimentoSeleccionadoId: Int = -1
    private var alimentoSeleccionadoNombre: String = ""

    // Objetivos del usuario
    private var objKcal = 2500.0
    private var objProt = 180.0
    private var objCarbs = 280.0
    private var objGrasas = 70.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nutricion)

        session = SessionManager(this)
        val dbHelper = DatabaseHelper(this)
        nutricionRepo = NutricionRepository(dbHelper)
        usuarioRepo = UsuarioRepository(dbHelper)

        val tvAlimento = findViewById<TextView>(R.id.tvAlimentoSeleccionado)
        val etGramos = findViewById<EditText>(R.id.etGramos)
        val btnElegirAlim = findViewById<Button>(R.id.btnElegirAlimento)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarIngesta)
        val btnIA = findViewById<Button>(R.id.btnIrIANutricion)
        val btnVolver = findViewById<Button>(R.id.btnVolverNutricion)

        val rv = findViewById<RecyclerView>(R.id.rvIngestasHoy)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = IngestaAdapter(ingestasHoy) { ingesta ->
            AlertDialog.Builder(this)
                .setTitle("Borrar ingesta")
                .setMessage("¿Borrar ${ingesta.alimentoNombre} (${"%.0f".format(ingesta.cantidadGramos)} g)?")
                .setPositiveButton("Borrar") { _, _ ->
                    if (nutricionRepo.borrarIngesta(ingesta.id)) refrescar()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
        rv.adapter = adapter

        btnElegirAlim.setOnClickListener {
            startActivityForResult(
                Intent(this, SelectorAlimentos::class.java),
                REQ_SELECTOR_ALIM
            )
        }

        btnGuardar.setOnClickListener {
            if (alimentoSeleccionadoId <= 0) {
                Toast.makeText(this, "Elige un alimento primero", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val gramos = etGramos.text.toString().toDoubleOrNull()
            if (gramos == null || gramos <= 0) {
                etGramos.error = "Introduce una cantidad válida (g)"
                return@setOnClickListener
            }
            val id = nutricionRepo.registrarIngesta(
                usuarioId = session.usuarioId(),
                alimentoId = alimentoSeleccionadoId,
                cantidadGramos = gramos
            )
            if (id > 0) {
                Toast.makeText(this, "Ingesta guardada", Toast.LENGTH_SHORT).show()
                etGramos.setText("")
                tvAlimento.text = "Ningún alimento seleccionado"
                alimentoSeleccionadoId = -1
                alimentoSeleccionadoNombre = ""
                refrescar()
            } else {
                Toast.makeText(this, "Error guardando ingesta", Toast.LENGTH_SHORT).show()
            }
        }

        btnIA.setOnClickListener {
            startActivity(Intent(this, ConsejosIA::class.java))
        }

        btnVolver.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        refrescar()
    }

    @Deprecated("Suficiente con startActivityForResult para el alcance del TFG")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_SELECTOR_ALIM && resultCode == RESULT_OK && data != null) {
            alimentoSeleccionadoId = data.getIntExtra("alimentoId", -1)
            alimentoSeleccionadoNombre = data.getStringExtra("alimentoNombre") ?: ""
            findViewById<TextView>(R.id.tvAlimentoSeleccionado).text =
                if (alimentoSeleccionadoId > 0) "🍽 $alimentoSeleccionadoNombre"
                else "Ningún alimento seleccionado"
        }
    }

    private fun refrescar() {
        // Cargar objetivos actualizados del perfil
        val u = usuarioRepo.buscarPorId(session.usuarioId())
        if (u != null) {
            objKcal = u.caloriasObjetivo.coerceAtLeast(1.0)
            objProt = u.proteinasObjetivo.coerceAtLeast(1.0)
            objCarbs = u.carbsObjetivo.coerceAtLeast(1.0)
            objGrasas = u.grasasObjetivo.coerceAtLeast(1.0)
        }

        // Totales de hoy
        val totales = nutricionRepo.totalesHoy(session.usuarioId())

        val pbCal = findViewById<ProgressBar>(R.id.pbCalorias)
        val pbProt = findViewById<ProgressBar>(R.id.pbProteinas)
        val pbCarbs = findViewById<ProgressBar>(R.id.pbCarbs)
        val pbGrasas = findViewById<ProgressBar>(R.id.pbGrasas)

        pintar(pbCal, findViewById(R.id.tvProgresoCalorias), "kcal", totales.calorias, objKcal)
        pintar(pbProt, findViewById(R.id.tvProgresoProteinas), "g P", totales.proteinas, objProt)
        pintar(pbCarbs, findViewById(R.id.tvProgresoCarbs), "g C", totales.carbs, objCarbs)
        pintar(pbGrasas, findViewById(R.id.tvProgresoGrasas), "g G", totales.grasas, objGrasas)

        // Lista de ingestas
        ingestasHoy.clear()
        ingestasHoy.addAll(nutricionRepo.ingestasDeHoy(session.usuarioId()))
        adapter.notifyDataSetChanged()

        val tvVacio = findViewById<TextView>(R.id.tvVacioIngestas)
        tvVacio.visibility = if (ingestasHoy.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun pintar(pb: ProgressBar, tv: TextView, unidad: String, actual: Double, objetivo: Double) {
        pb.max = objetivo.toInt().coerceAtLeast(1)
        pb.progress = actual.toInt().coerceIn(0, pb.max)
        tv.text = "${"%.0f".format(actual)} / ${"%.0f".format(objetivo)} $unidad"
    }

    companion object {
        private const val REQ_SELECTOR_ALIM = 301
    }
}
