package com.example.diaaia

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.diaaia.model.DatabaseHelper
import com.example.diaaia.model.Ejercicio
import com.example.diaaia.repository.EjercicioRepository

/**
 * Selector de un ejercicio del catálogo. Se lanza con startActivityForResult y
 * devuelve el `ejercicioId` seleccionado.
 */
class SelectorEjercicios : AppCompatActivity() {

    private val todos = mutableListOf<Ejercicio>()
    private val filtrados = mutableListOf<Ejercicio>()
    private lateinit var adapter: CatalogoEjerciciosAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selector_ejercicios)

        val dbHelper = DatabaseHelper(this)
        val repo = EjercicioRepository(dbHelper)

        val etBuscar = findViewById<EditText>(R.id.etBuscarEjercicio)
        val rv = findViewById<RecyclerView>(R.id.rvCatalogoEjercicios)
        val btnCancelar = findViewById<Button>(R.id.btnCancelarSelector)

        todos.addAll(repo.listarTodos())
        filtrados.addAll(todos)

        adapter = CatalogoEjerciciosAdapter(filtrados) { ej ->
            val resultado = Intent().apply { putExtra("ejercicioId", ej.id) }
            setResult(RESULT_OK, resultado)
            finish()
        }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val q = s.toString().trim().lowercase()
                filtrados.clear()
                if (q.isEmpty()) {
                    filtrados.addAll(todos)
                } else {
                    filtrados.addAll(todos.filter {
                        it.nombre.lowercase().contains(q) ||
                            it.musculoPrimario.lowercase().contains(q)
                    })
                }
                adapter.notifyDataSetChanged()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnCancelar.setOnClickListener { finish() }
    }

    private class CatalogoEjerciciosAdapter(
        private val lista: List<Ejercicio>,
        private val onClick: (Ejercicio) -> Unit
    ) : RecyclerView.Adapter<CatalogoEjerciciosAdapter.VH>() {

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvNombre: TextView = v.findViewById(R.id.tvNombreCatEj)
            val tvMusculo: TextView = v.findViewById(R.id.tvMusculoCatEj)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_catalogo_ejercicio, parent, false)
            return VH(v)
        }

        override fun getItemCount(): Int = lista.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val ej = lista[position]
            holder.tvNombre.text = ej.nombre
            holder.tvMusculo.text = if (ej.musculoSecundario.isNullOrBlank()) {
                ej.musculoPrimario
            } else "${ej.musculoPrimario} · ${ej.musculoSecundario}"
            holder.itemView.setOnClickListener { onClick(ej) }
        }
    }
}
