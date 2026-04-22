package com.example.diaaia

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.diaaia.model.DatabaseHelper
import com.example.diaaia.model.SessionManager
import com.example.diaaia.model.Usuario
import com.example.diaaia.repository.EntrenadorRepository

/**
 * Pantalla de gestión de clientes del entrenador (requisito DESEABLE del TFG).
 *
 * Permite ver los clientes ya vinculados (con opción a desvincular) y añadir
 * nuevos clientes de entre los usuarios con rol "cliente" disponibles.
 *
 * Solo se muestra desde MenuPrincipal cuando [SessionManager.esEntrenador] es true.
 */
class MisClientes : AppCompatActivity() {

    private lateinit var entrenadorRepo: EntrenadorRepository
    private lateinit var session: SessionManager
    private lateinit var rvClientes: RecyclerView
    private lateinit var tvVacio: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_clientes)

        session = SessionManager(this)
        val dbHelper = DatabaseHelper(this)
        entrenadorRepo = EntrenadorRepository(dbHelper)

        if (!session.esEntrenador()) {
            Toast.makeText(this, "Solo accesible para entrenadores", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        rvClientes = findViewById(R.id.rvMisClientes)
        tvVacio = findViewById(R.id.tvVacioClientes)
        val btnAnadir = findViewById<Button>(R.id.btnAnadirCliente)
        val btnVolver = findViewById<Button>(R.id.btnVolverClientes)

        rvClientes.layoutManager = LinearLayoutManager(this)

        btnAnadir.setOnClickListener { mostrarDialogoAnadirCliente() }
        btnVolver.setOnClickListener { finish() }

        recargarListado()
    }

    override fun onResume() {
        super.onResume()
        recargarListado()
    }

    private fun recargarListado() {
        val clientes = entrenadorRepo.clientesDeEntrenador(session.usuarioId())
        if (clientes.isEmpty()) {
            rvClientes.visibility = View.GONE
            tvVacio.visibility = View.VISIBLE
        } else {
            rvClientes.visibility = View.VISIBLE
            tvVacio.visibility = View.GONE
            rvClientes.adapter = ClientesAdapter(clientes) { cliente ->
                confirmarDesvincular(cliente)
            }
        }
    }

    private fun confirmarDesvincular(cliente: Usuario) {
        AlertDialog.Builder(this)
            .setTitle("Quitar cliente")
            .setMessage("¿Quieres dejar de entrenar a ${cliente.nombre}?")
            .setPositiveButton("Sí") { _, _ ->
                if (entrenadorRepo.desvincular(session.usuarioId(), cliente.id)) {
                    Toast.makeText(this, "${cliente.nombre} desvinculado", Toast.LENGTH_SHORT).show()
                    recargarListado()
                } else {
                    Toast.makeText(this, "No se pudo desvincular", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun mostrarDialogoAnadirCliente() {
        val disponibles = entrenadorRepo.clientesDisponibles(session.usuarioId())
        if (disponibles.isEmpty()) {
            Toast.makeText(
                this,
                "No hay clientes disponibles para añadir",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val nombres = disponibles.map { it.nombre }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Elige un cliente")
            .setItems(nombres) { _, which ->
                val cli = disponibles[which]
                val idVinc = entrenadorRepo.vincular(session.usuarioId(), cli.id)
                if (idVinc > 0) {
                    Toast.makeText(this, "${cli.nombre} añadido", Toast.LENGTH_SHORT).show()
                    recargarListado()
                } else {
                    Toast.makeText(this, "Error al vincular cliente", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
