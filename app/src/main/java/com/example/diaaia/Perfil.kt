package com.example.diaaia

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.diaaia.model.DatabaseHelper
import com.example.diaaia.model.SessionManager
import com.example.diaaia.repository.UsuarioRepository

/**
 * Pantalla de Perfil del usuario.
 *
 * Permite editar peso corporal, macros objetivo y meta. Cubre el requisito MVP
 * del TFG "Autenticación y Perfil" que estaba parcialmente implementado.
 */
class Perfil : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        val session = SessionManager(this)
        val dbHelper = DatabaseHelper(this)
        val usuarioRepo = UsuarioRepository(dbHelper)

        val etPeso = findViewById<EditText>(R.id.etPesoCorporal)
        val etCalorias = findViewById<EditText>(R.id.etCaloriasObjetivo)
        val etProteinas = findViewById<EditText>(R.id.etProteinasObjetivo)
        val etCarbs = findViewById<EditText>(R.id.etCarbsObjetivo)
        val etGrasas = findViewById<EditText>(R.id.etGrasasObjetivo)
        val spMeta = findViewById<Spinner>(R.id.spMeta)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarPerfil)
        val btnVolver = findViewById<Button>(R.id.btnVolverPerfil)

        val metas = arrayOf("hipertrofia", "definicion", "mantenimiento")
        spMeta.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, metas)

        val usuario = usuarioRepo.buscarPorId(session.usuarioId())
        if (usuario == null) {
            Toast.makeText(this, "Error cargando perfil", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        etPeso.setText(usuario.pesoCorporal.toString())
        etCalorias.setText(usuario.caloriasObjetivo.toInt().toString())
        etProteinas.setText(usuario.proteinasObjetivo.toInt().toString())
        etCarbs.setText(usuario.carbsObjetivo.toInt().toString())
        etGrasas.setText(usuario.grasasObjetivo.toInt().toString())
        spMeta.setSelection(metas.indexOf(usuario.meta).coerceAtLeast(0))

        btnGuardar.setOnClickListener {
            val peso = etPeso.text.toString().toDoubleOrNull()
            val cal = etCalorias.text.toString().toDoubleOrNull()
            val prot = etProteinas.text.toString().toDoubleOrNull()
            val carbs = etCarbs.text.toString().toDoubleOrNull()
            val grasas = etGrasas.text.toString().toDoubleOrNull()

            if (peso == null || peso <= 0) { etPeso.error = "Peso inválido"; return@setOnClickListener }
            if (cal == null || cal < 1000) { etCalorias.error = "Mínimo 1000 kcal"; return@setOnClickListener }
            if (prot == null || prot <= 0) { etProteinas.error = "Valor inválido"; return@setOnClickListener }
            if (carbs == null || carbs < 0) { etCarbs.error = "Valor inválido"; return@setOnClickListener }
            if (grasas == null || grasas < 0) { etGrasas.error = "Valor inválido"; return@setOnClickListener }

            usuario.pesoCorporal = peso
            usuario.caloriasObjetivo = cal
            usuario.proteinasObjetivo = prot
            usuario.carbsObjetivo = carbs
            usuario.grasasObjetivo = grasas
            usuario.meta = spMeta.selectedItem.toString()

            if (usuarioRepo.actualizarPerfil(usuario)) {
                Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show()
            }
        }

        btnVolver.setOnClickListener { finish() }
    }
}
