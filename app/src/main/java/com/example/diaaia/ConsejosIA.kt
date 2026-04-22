package com.example.diaaia

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.diaaia.model.Alimento
import com.example.diaaia.model.DatabaseHelper
import com.example.diaaia.model.SessionManager
import com.example.diaaia.repository.EntrenamientoRepository
import com.example.diaaia.repository.NutricionRepository
import com.example.diaaia.repository.UsuarioRepository
import com.example.diaaia.service.ProgressionService
import com.example.diaaia.service.RecomendacionNutricionalService

/**
 * Pantalla del asistente IA.
 *
 * Presenta dos bloques de "IA basada en reglas" que cubren los requisitos MVP
 * y deseables del TFG:
 *  - [RecomendacionNutricionalService]: detecta déficit de macros y sugiere
 *    alimentos concretos del catálogo.
 *  - [ProgressionService]: (vía SesionEntrenamiento) aplica sobrecarga progresiva.
 *
 * Todo es determinista y funciona sin internet, tal y como pide el TFG.
 */
class ConsejosIA : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_consejos_ia)

        val session = SessionManager(this)
        val dbHelper = DatabaseHelper(this)
        val usuarioRepo = UsuarioRepository(dbHelper)
        val nutricionRepo = NutricionRepository(dbHelper)
        val entrenamientoRepo = EntrenamientoRepository(dbHelper)
        val nutricionalService = RecomendacionNutricionalService(nutricionRepo)

        val tvResumen = findViewById<TextView>(R.id.tvAnalisisIA)
        val tvConsejo = findViewById<TextView>(R.id.tvConsejoIA)
        val llAlimentos = findViewById<LinearLayout>(R.id.llAlimentosRecomendados)
        val tvEntreno = findViewById<TextView>(R.id.tvConsejoEntreno)
        val btnCerrar = findViewById<Button>(R.id.btnCerrarIA)

        val usuario = usuarioRepo.buscarPorId(session.usuarioId())
        if (usuario == null) {
            tvResumen.text = "No se pudo cargar tu perfil."
            tvConsejo.text = ""
            btnCerrar.setOnClickListener { finish() }
            return
        }

        // 1) Análisis nutricional basado en reglas
        val totales = nutricionRepo.totalesHoy(usuario.id)
        tvResumen.text = buildString {
            append("Kcal: ${"%.0f".format(totales.calorias)} / ${"%.0f".format(usuario.caloriasObjetivo)} · ")
            append("P: ${"%.0f".format(totales.proteinas)} / ${"%.0f".format(usuario.proteinasObjetivo)} g · ")
            append("C: ${"%.0f".format(totales.carbs)} / ${"%.0f".format(usuario.carbsObjetivo)} g · ")
            append("G: ${"%.0f".format(totales.grasas)} / ${"%.0f".format(usuario.grasasObjetivo)} g")
        }

        val reco = nutricionalService.recomendar(usuario, totales)
        tvConsejo.text = reco.mensaje

        llAlimentos.removeAllViews()
        if (reco.alimentos.isNotEmpty()) {
            for (a in reco.alimentos) {
                llAlimentos.addView(crearChipAlimento(a, llAlimentos))
            }
        }

        // 2) Consejo basado en entrenamientos recientes
        tvEntreno.text = construirConsejoEntreno(
            usuarioId = usuario.id,
            entrenamientoRepo = entrenamientoRepo
        )

        btnCerrar.setOnClickListener { finish() }
    }

    private fun crearChipAlimento(a: Alimento, parent: ViewGroup): View {
        val v = LayoutInflater.from(this).inflate(R.layout.item_alimento_recomendado, parent, false)
        v.findViewById<TextView>(R.id.tvChipNombre).text = a.nombre
        v.findViewById<TextView>(R.id.tvChipMacros).text =
            "${"%.0f".format(a.caloriasPor100g)} kcal · " +
            "P ${"%.1f".format(a.proteinasPor100g)} · " +
            "C ${"%.1f".format(a.carbsPor100g)} · " +
            "G ${"%.1f".format(a.grasasPor100g)} (por 100g)"
        return v
    }

    /**
     * Regla simple para dar feedback de entrenamiento sin entrar en cálculos de 1RM:
     * si no hay sesiones en 3 días, recordar que toca entrenar.
     */
    private fun construirConsejoEntreno(
        usuarioId: Int,
        entrenamientoRepo: EntrenamientoRepository
    ): String {
        val sesiones = entrenamientoRepo.sesionesUsuario(usuarioId)
        return when {
            sesiones.isEmpty() ->
                "Aún no tienes sesiones registradas. Crea una rutina y empieza a entrenar para " +
                "que la IA pueda sugerirte progresión."
            sesiones.size < 4 ->
                "Llevas ${sesiones.size} sesión(es) registrada(s). Añade más constancia para que la " +
                "IA pueda detectar patrones de progresión."
            else ->
                "Llevas ${sesiones.size} sesiones. La IA aplicará sobrecarga progresiva en cada " +
                "ejercicio en base a tus últimos entrenamientos."
        }
    }
}
