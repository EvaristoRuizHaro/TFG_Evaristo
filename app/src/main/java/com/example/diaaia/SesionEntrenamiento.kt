package com.example.diaaia

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.diaaia.model.DatabaseHelper
import com.example.diaaia.model.DatosGlobales
import com.example.diaaia.model.Ejercicio
import com.example.diaaia.model.RegistroSet
import com.example.diaaia.model.RutinaEjercicio
import com.example.diaaia.model.SessionManager
import com.example.diaaia.repository.EjercicioRepository
import com.example.diaaia.repository.EntrenamientoRepository
import com.example.diaaia.repository.RutinaRepository
import com.example.diaaia.service.ProgressionService

/**
 * Pantalla principal de Entrenamiento. Aquí el usuario registra sus sets uno a uno:
 * peso, repeticiones realizadas, y los guarda en la tabla `registro_set` asociados
 * a la sesión (`sesion_id`) y al ejercicio (`ejercicio_id`).
 *
 * La sesión se crea en `onCreate` (obteniendo un id autoincremental) para que cada
 * set registrado tenga inmediatamente su `sesion_id` válido. Si el usuario cancela
 * y no ha guardado ningún set, la sesión se borra al salir para no dejar basura.
 *
 * Soporta:
 *  - Entrenamientos basados en rutina: los ejercicios se cargan automáticamente
 *    de la rutina seleccionada con sus series/reps planeadas.
 *  - Entrenamientos libres: se puede añadir cualquier ejercicio del catálogo.
 *
 * Cubre requisito MVP del TFG ("Registro de Entrenamiento") y se apoya en el
 * servicio [ProgressionService] para la IA de sobrecarga progresiva (deseable).
 */
class SesionEntrenamiento : AppCompatActivity() {

    private lateinit var entrenamientoRepo: EntrenamientoRepository
    private lateinit var rutinaRepo: RutinaRepository
    private lateinit var ejercicioRepo: EjercicioRepository
    private lateinit var session: SessionManager
    private lateinit var progression: ProgressionService

    private var sesionId: Int = -1
    private var rutinaId: Int = -1

    /** Ejercicios que forman parte de la sesión, en orden. */
    private val ejerciciosSesion = mutableListOf<EjercicioEnSesion>()

    /** Sets que ya han sido persistidos (para recalcular la IA al vuelo). */
    private val setsGuardados = mutableListOf<RegistroSet>()

    private lateinit var llEjercicios: LinearLayout
    private var numeroSetsGuardados = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sesion_entrenamiento)

        session = SessionManager(this)
        val dbHelper = DatabaseHelper(this)
        entrenamientoRepo = EntrenamientoRepository(dbHelper)
        rutinaRepo = RutinaRepository(dbHelper)
        ejercicioRepo = EjercicioRepository(dbHelper)
        progression = ProgressionService(entrenamientoRepo)

        rutinaId = intent.getIntExtra("rutinaId", -1)
        val nombreRutina = DatosGlobales.rutinaSeleccionadaNombre.ifBlank {
            if (rutinaId > 0) "Rutina" else "Sesión libre"
        }

        val tvTitulo = findViewById<TextView>(R.id.tvTituloSesion)
        llEjercicios = findViewById(R.id.llEjerciciosSesion)
        val btnAnadir = findViewById<Button>(R.id.btnAnadirEjSesion)
        val btnFinalizar = findViewById<Button>(R.id.btnFinalizarSesion)
        val btnCancelar = findViewById<Button>(R.id.btnCancelarSesion)

        tvTitulo.text = "🏋️ $nombreRutina"

        // Crear la sesión en BD inmediatamente para tener sesion_id
        val sid = entrenamientoRepo.crearSesion(
            usuarioId = session.usuarioId(),
            rutinaId = if (rutinaId > 0) rutinaId else null,
            nombreRutina = nombreRutina
        )
        if (sid <= 0) {
            Toast.makeText(this, "Error creando la sesión", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        sesionId = sid.toInt()
        DatosGlobales.sesionActivaId = sesionId

        // Cargar ejercicios de la rutina (si aplica)
        if (rutinaId > 0) {
            val rutina = rutinaRepo.obtenerRutinaCompleta(rutinaId)
            if (rutina != null) {
                for (ej in rutina.ejercicios) {
                    agregarEjercicioALaSesion(
                        ejercicioId = ej.ejercicioId,
                        nombre = ej.ejercicioNombre,
                        musculo = ej.musculoPrimario,
                        seriesPlaneadas = ej.seriesPlaneadas,
                        repsPlaneadas = ej.repsPlaneadas
                    )
                }
            }
        }

        btnAnadir.setOnClickListener {
            startActivityForResult(
                Intent(this, SelectorEjercicios::class.java),
                REQ_SELECTOR_SET
            )
        }

        btnFinalizar.setOnClickListener {
            if (numeroSetsGuardados == 0) {
                AlertDialog.Builder(this)
                    .setTitle("No has guardado ningún set")
                    .setMessage("Si finalizas ahora la sesión se borrará. ¿Seguro?")
                    .setPositiveButton("Sí, salir") { _, _ ->
                        entrenamientoRepo.borrarSesion(sesionId)
                        DatosGlobales.limpiar()
                        finish()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
                return@setOnClickListener
            }
            mostrarResumenYSugerencias()
        }

        btnCancelar.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cancelar sesión")
                .setMessage("Si cancelas ahora se perderá la sesión completa. ¿Seguro?")
                .setPositiveButton("Sí, cancelar") { _, _ ->
                    entrenamientoRepo.borrarSesion(sesionId)
                    DatosGlobales.limpiar()
                    finish()
                }
                .setNegativeButton("Seguir entrenando", null)
                .show()
        }
    }

    @Deprecated("Suficiente con startActivityForResult para el alcance del TFG")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_SELECTOR_SET && resultCode == RESULT_OK && data != null) {
            val ejId = data.getIntExtra("ejercicioId", -1)
            if (ejId <= 0) return
            if (ejerciciosSesion.any { it.ejercicioId == ejId }) {
                Toast.makeText(this, "Ya está añadido en esta sesión", Toast.LENGTH_SHORT).show()
                return
            }
            val ej = ejercicioRepo.buscarPorId(ejId) ?: return
            agregarEjercicioALaSesion(
                ejercicioId = ej.id,
                nombre = ej.nombre,
                musculo = ej.musculoPrimario,
                seriesPlaneadas = 3,
                repsPlaneadas = 10
            )
        }
    }

    private fun agregarEjercicioALaSesion(
        ejercicioId: Int,
        nombre: String,
        musculo: String,
        seriesPlaneadas: Int,
        repsPlaneadas: Int
    ) {
        val ejCard = LayoutInflater.from(this)
            .inflate(R.layout.item_sesion_ejercicio, llEjercicios, false)

        val tvNombre = ejCard.findViewById<TextView>(R.id.tvNombreEjSesion)
        val tvMusculo = ejCard.findViewById<TextView>(R.id.tvMusculoEjSesion)
        val llSets = ejCard.findViewById<LinearLayout>(R.id.llSetsEj)
        val btnAddSet = ejCard.findViewById<Button>(R.id.btnAddSet)
        val btnIA = ejCard.findViewById<Button>(R.id.btnSugerenciaIA)

        tvNombre.text = nombre
        tvMusculo.text = musculo

        val ejEnSesion = EjercicioEnSesion(
            ejercicioId = ejercicioId,
            nombre = nombre,
            musculo = musculo,
            seriesPlaneadas = seriesPlaneadas,
            repsPlaneadas = repsPlaneadas,
            container = llSets
        )

        // Sets iniciales según la rutina
        for (i in 1..seriesPlaneadas) {
            anadirFilaDeSet(ejEnSesion, i)
        }

        btnAddSet.setOnClickListener {
            val siguiente = ejEnSesion.ultimoNumeroSet + 1
            anadirFilaDeSet(ejEnSesion, siguiente)
        }

        btnIA.setOnClickListener {
            mostrarSugerenciaIA(ejEnSesion)
        }

        ejerciciosSesion.add(ejEnSesion)
        llEjercicios.addView(ejCard)
    }

    private fun anadirFilaDeSet(ej: EjercicioEnSesion, numeroSet: Int) {
        ej.ultimoNumeroSet = maxOf(ej.ultimoNumeroSet, numeroSet)

        val row = LayoutInflater.from(this)
            .inflate(R.layout.item_set_row, ej.container, false)

        val tvLabel = row.findViewById<TextView>(R.id.tvSetLabel)
        val etPeso = row.findViewById<EditText>(R.id.etPesoSet)
        val etReps = row.findViewById<EditText>(R.id.etRepsSet)
        val btnGuardar = row.findViewById<ImageButton>(R.id.btnGuardarSet)

        tvLabel.text = "Set $numeroSet"
        etReps.hint = "reps (plan: ${ej.repsPlaneadas})"
        etPeso.hint = "kg"
        etPeso.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        etReps.inputType = InputType.TYPE_CLASS_NUMBER

        btnGuardar.setOnClickListener {
            val peso = etPeso.text.toString().toDoubleOrNull()
            val reps = etReps.text.toString().toIntOrNull()

            if (peso == null || peso < 0) {
                etPeso.error = "Peso inválido"
                return@setOnClickListener
            }
            if (reps == null || reps < 0) {
                etReps.error = "Reps inválidas"
                return@setOnClickListener
            }

            val id = entrenamientoRepo.registrarSet(
                sesionId = sesionId,
                ejercicioId = ej.ejercicioId,
                numeroSet = numeroSet,
                peso = peso,
                repsPlaneadas = ej.repsPlaneadas,
                repsReales = reps
            )
            if (id > 0) {
                setsGuardados.add(
                    RegistroSet(
                        id = id.toInt(),
                        sesionId = sesionId,
                        ejercicioId = ej.ejercicioId,
                        ejercicioNombre = ej.nombre,
                        numeroSet = numeroSet,
                        peso = peso,
                        repsPlaneadas = ej.repsPlaneadas,
                        repsReales = reps
                    )
                )
                numeroSetsGuardados++
                // Feedback visual: desactivar fila y marcar como guardado
                etPeso.isEnabled = false
                etReps.isEnabled = false
                btnGuardar.isEnabled = false
                btnGuardar.setImageResource(android.R.drawable.checkbox_on_background)
                row.setBackgroundColor(0xFFE8F5E9.toInt())
            } else {
                Toast.makeText(this, "Error guardando set", Toast.LENGTH_SHORT).show()
            }
        }

        ej.container.addView(row)
    }

    private fun mostrarSugerenciaIA(ej: EjercicioEnSesion) {
        val setsEj = setsGuardados.filter { it.ejercicioId == ej.ejercicioId }
        if (setsEj.isEmpty()) {
            Toast.makeText(
                this,
                "Guarda al menos un set de este ejercicio para ver la sugerencia",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val sug = progression.sugerirProximaSesion(
            setsDeHoy = setsEj,
            usuarioId = session.usuarioId(),
            ejercicioId = ej.ejercicioId,
            sesionActualId = sesionId
        )
        val icono = when (sug.tipo) {
            ProgressionService.Sugerencia.Tipo.SUBIR -> "⬆️"
            ProgressionService.Sugerencia.Tipo.MANTENER -> "➡️"
            ProgressionService.Sugerencia.Tipo.BAJAR -> "⬇️"
        }
        val pesoStr = if (sug.pesoSugerido % 1.0 == 0.0) {
            sug.pesoSugerido.toInt().toString()
        } else "%.2f".format(sug.pesoSugerido)

        AlertDialog.Builder(this)
            .setTitle("$icono Sugerencia IA: ${ej.nombre}")
            .setMessage(
                "${sug.razon}\n\nPróxima sesión: $pesoStr kg × ${sug.repsSugeridas} reps"
            )
            .setPositiveButton("Entendido", null)
            .show()
    }

    private fun mostrarResumenYSugerencias() {
        val totalEj = ejerciciosSesion.size
        val totalSets = numeroSetsGuardados
        val volumen = setsGuardados.sumOf { it.peso * it.repsReales }

        val resumen = buildString {
            append("Sesión guardada ✅\n\n")
            append("Ejercicios: $totalEj\n")
            append("Series registradas: $totalSets\n")
            append("Volumen total: ${"%.0f".format(volumen)} kg\n\n")

            append("— Sugerencias para la próxima sesión —\n\n")
            val ejerciciosConSets = ejerciciosSesion.filter { ej ->
                setsGuardados.any { it.ejercicioId == ej.ejercicioId }
            }
            if (ejerciciosConSets.isEmpty()) {
                append("Registra al menos un set para recibir sugerencias.")
            } else {
                for (ej in ejerciciosConSets) {
                    val sets = setsGuardados.filter { it.ejercicioId == ej.ejercicioId }
                    val sug = progression.sugerirProximaSesion(
                        setsDeHoy = sets,
                        usuarioId = session.usuarioId(),
                        ejercicioId = ej.ejercicioId,
                        sesionActualId = sesionId
                    )
                    val icono = when (sug.tipo) {
                        ProgressionService.Sugerencia.Tipo.SUBIR -> "⬆️"
                        ProgressionService.Sugerencia.Tipo.MANTENER -> "➡️"
                        ProgressionService.Sugerencia.Tipo.BAJAR -> "⬇️"
                    }
                    val pesoStr = if (sug.pesoSugerido % 1.0 == 0.0) {
                        sug.pesoSugerido.toInt().toString()
                    } else "%.2f".format(sug.pesoSugerido)
                    append("$icono ${ej.nombre}: $pesoStr kg × ${sug.repsSugeridas}\n")
                }
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Sesión finalizada")
            .setMessage(resumen)
            .setCancelable(false)
            .setPositiveButton("OK") { _, _ ->
                DatosGlobales.limpiar()
                finish()
            }
            .show()
    }

    /** Estado en memoria por ejercicio dentro de la sesión. */
    private data class EjercicioEnSesion(
        val ejercicioId: Int,
        val nombre: String,
        val musculo: String,
        val seriesPlaneadas: Int,
        val repsPlaneadas: Int,
        val container: LinearLayout,
        var ultimoNumeroSet: Int = 0
    )

    companion object {
        private const val REQ_SELECTOR_SET = 201
    }
}
