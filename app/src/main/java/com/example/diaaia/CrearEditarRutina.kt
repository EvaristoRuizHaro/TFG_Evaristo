package com.example.diaaia

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.diaaia.model.DatabaseHelper
import com.example.diaaia.model.RutinaEjercicio
import com.example.diaaia.model.SessionManager
import com.example.diaaia.repository.EjercicioRepository
import com.example.diaaia.repository.RutinaRepository

/**
 * Activity para crear una rutina nueva o editar una existente.
 *
 * Recibe opcionalmente `rutinaId` como extra. Si está presente, carga la rutina
 * para edición. Si no, es una creación.
 *
 * Los ejercicios se añaden abriendo [SelectorEjercicios] que devuelve un `ejercicioId`
 * mediante startActivityForResult.
 */
class CrearEditarRutina : AppCompatActivity() {

    private lateinit var rutinaRepo: RutinaRepository
    private lateinit var ejercicioRepo: EjercicioRepository
    private lateinit var session: SessionManager

    private var rutinaId: Int = -1
    private val ejercicios = mutableListOf<RutinaEjercicio>()
    private lateinit var adapter: RutinaEjerciciosAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_editar_rutina)

        session = SessionManager(this)
        val dbHelper = DatabaseHelper(this)
        rutinaRepo = RutinaRepository(dbHelper)
        ejercicioRepo = EjercicioRepository(dbHelper)

        rutinaId = intent.getIntExtra("rutinaId", -1)

        val tvTitulo = findViewById<TextView>(R.id.tvTituloRutina)
        val etNombre = findViewById<EditText>(R.id.etNombreRutina)
        val etDescripcion = findViewById<EditText>(R.id.etDescripcionRutina)
        val btnAnadirEj = findViewById<Button>(R.id.btnAnadirEjercicio)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarRutina)
        val btnCancelar = findViewById<Button>(R.id.btnCancelarRutina)
        val rv = findViewById<RecyclerView>(R.id.rvEjerciciosRutina)

        rv.layoutManager = LinearLayoutManager(this)
        adapter = RutinaEjerciciosAdapter(ejercicios) { index ->
            ejercicios.removeAt(index)
            adapter.notifyItemRemoved(index)
        }
        rv.adapter = adapter

        if (rutinaId > 0) {
            tvTitulo.text = "Editar rutina"
            val existente = rutinaRepo.obtenerRutinaCompleta(rutinaId)
            if (existente != null) {
                etNombre.setText(existente.nombre)
                etDescripcion.setText(existente.descripcion)
                ejercicios.addAll(existente.ejercicios)
                adapter.notifyDataSetChanged()
            }
        } else {
            tvTitulo.text = "Nueva rutina"
        }

        btnAnadirEj.setOnClickListener {
            startActivityForResult(
                Intent(this, SelectorEjercicios::class.java),
                REQ_SELECTOR
            )
        }

        btnGuardar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val descripcion = etDescripcion.text.toString().trim()
            if (nombre.isEmpty()) {
                etNombre.error = "Pon un nombre a tu rutina"
                return@setOnClickListener
            }
            if (ejercicios.isEmpty()) {
                Toast.makeText(this, "Añade al menos un ejercicio", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val idFinal = if (rutinaId > 0) {
                rutinaRepo.actualizarRutina(rutinaId, nombre, descripcion)
                rutinaId
            } else {
                rutinaRepo.crearRutina(session.usuarioId(), nombre, descripcion).toInt()
            }
            if (idFinal > 0) {
                rutinaRepo.reemplazarEjerciciosDeRutina(idFinal, ejercicios)
                Toast.makeText(this, "Rutina guardada", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Error al guardar la rutina", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancelar.setOnClickListener { finish() }

        refrescarContador()
    }

    @Deprecated("Migrable a ActivityResultContracts más adelante; para el TFG con startActivityForResult es suficiente")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_SELECTOR && resultCode == RESULT_OK && data != null) {
            val ejId = data.getIntExtra("ejercicioId", -1)
            if (ejId > 0 && ejercicios.none { it.ejercicioId == ejId }) {
                val ej = ejercicioRepo.buscarPorId(ejId) ?: return
                ejercicios.add(
                    RutinaEjercicio(
                        id = 0,
                        rutinaId = rutinaId,
                        ejercicioId = ej.id,
                        ejercicioNombre = ej.nombre,
                        musculoPrimario = ej.musculoPrimario,
                        orden = ejercicios.size,
                        seriesPlaneadas = 3,
                        repsPlaneadas = 10
                    )
                )
                adapter.notifyItemInserted(ejercicios.size - 1)
                refrescarContador()
            } else if (ejId > 0) {
                Toast.makeText(this, "Ese ejercicio ya está en la rutina", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun refrescarContador() {
        val tvVacio = findViewById<TextView>(R.id.tvVacioEjercicios)
        tvVacio.visibility = if (ejercicios.isEmpty()) View.VISIBLE else View.GONE
    }

    companion object {
        private const val REQ_SELECTOR = 101
    }
}
