package com.example.diaaia

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.diaaia.model.DatabaseHelper
import com.example.diaaia.model.Rutina
import com.example.diaaia.model.SessionManager
import com.example.diaaia.repository.RutinaRepository

/**
 * Listado de rutinas del usuario logueado. Punto de entrada al CRUD.
 * Desde aquí se crea una nueva, o se toca una existente para editarla/borrarla.
 */
class GestionRutinas : AppCompatActivity() {

    private lateinit var rutinaRepo: RutinaRepository
    private lateinit var session: SessionManager
    private lateinit var adapter: RutinasAdapter
    private val rutinas = mutableListOf<Rutina>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gestion_rutinas)

        session = SessionManager(this)
        val dbHelper = DatabaseHelper(this)
        rutinaRepo = RutinaRepository(dbHelper)

        val rv = findViewById<RecyclerView>(R.id.rvRutinas)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = RutinasAdapter(
            rutinas,
            onEditar = { rutina ->
                val intent = Intent(this, CrearEditarRutina::class.java)
                intent.putExtra("rutinaId", rutina.id)
                startActivity(intent)
            },
            onBorrar = { rutina ->
                if (rutinaRepo.borrarRutina(rutina.id)) {
                    recargar()
                }
            }
        )
        rv.adapter = adapter

        findViewById<Button>(R.id.btnNuevaRutina).setOnClickListener {
            startActivity(Intent(this, CrearEditarRutina::class.java))
        }
        findViewById<Button>(R.id.btnVolverRutinas).setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        recargar()
    }

    private fun recargar() {
        rutinas.clear()
        rutinas.addAll(rutinaRepo.listarRutinasUsuario(session.usuarioId()))
        adapter.notifyDataSetChanged()

        val tvVacio = findViewById<TextView>(R.id.tvVacioRutinas)
        tvVacio.visibility = if (rutinas.isEmpty()) View.VISIBLE else View.GONE
    }
}
