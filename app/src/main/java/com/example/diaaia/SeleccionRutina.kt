package com.example.diaaia

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.diaaia.model.DatabaseHelper
import com.example.diaaia.model.DatosGlobales
import com.example.diaaia.model.Rutina
import com.example.diaaia.model.SessionManager
import com.example.diaaia.repository.RutinaRepository

/**
 * Pantalla intermedia antes de entrenar. Muestra las rutinas del usuario y
 * permite elegir con cuál va a entrenar hoy. También ofrece la opción de
 * "Entrenamiento libre" (sesión sin rutina preconfigurada).
 */
class SeleccionRutina : AppCompatActivity() {

    private lateinit var rutinaRepo: RutinaRepository
    private lateinit var session: SessionManager
    private val rutinas = mutableListOf<Rutina>()
    private lateinit var adapter: SeleccionRutinaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seleccion_rutina)

        session = SessionManager(this)
        val dbHelper = DatabaseHelper(this)
        rutinaRepo = RutinaRepository(dbHelper)

        val rv = findViewById<RecyclerView>(R.id.rvSeleccionRutina)
        val tvVacio = findViewById<TextView>(R.id.tvVacioSeleccion)
        val btnLibre = findViewById<Button>(R.id.btnEntrenoLibre)
        val btnVolver = findViewById<Button>(R.id.btnVolverSeleccion)

        rv.layoutManager = LinearLayoutManager(this)
        adapter = SeleccionRutinaAdapter(rutinas) { rutina ->
            DatosGlobales.rutinaSeleccionadaId = rutina.id
            DatosGlobales.rutinaSeleccionadaNombre = rutina.nombre
            val intent = Intent(this, SesionEntrenamiento::class.java)
            intent.putExtra("rutinaId", rutina.id)
            startActivity(intent)
            finish()
        }
        rv.adapter = adapter

        rutinas.clear()
        rutinas.addAll(rutinaRepo.listarRutinasUsuario(session.usuarioId()))
        adapter.notifyDataSetChanged()
        tvVacio.visibility = if (rutinas.isEmpty()) View.VISIBLE else View.GONE

        btnLibre.setOnClickListener {
            DatosGlobales.rutinaSeleccionadaId = -1
            DatosGlobales.rutinaSeleccionadaNombre = "Sesión libre"
            val intent = Intent(this, SesionEntrenamiento::class.java)
            intent.putExtra("rutinaId", -1)
            startActivity(intent)
            finish()
        }

        btnVolver.setOnClickListener { finish() }
    }

    private class SeleccionRutinaAdapter(
        private val lista: List<Rutina>,
        private val onClick: (Rutina) -> Unit
    ) : RecyclerView.Adapter<SeleccionRutinaAdapter.VH>() {

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvNombre: TextView = v.findViewById(R.id.tvNombreSelRutina)
            val tvDetalle: TextView = v.findViewById(R.id.tvDetalleSelRutina)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_seleccion_rutina, parent, false)
            return VH(v)
        }

        override fun getItemCount(): Int = lista.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val r = lista[position]
            holder.tvNombre.text = r.nombre
            holder.tvDetalle.text = if (r.descripcion.isBlank()) {
                "Creada el ${r.fechaCreacion}"
            } else r.descripcion
            holder.itemView.setOnClickListener { onClick(r) }
        }
    }
}
