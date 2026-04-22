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
import com.example.diaaia.model.Alimento
import com.example.diaaia.model.DatabaseHelper
import com.example.diaaia.repository.NutricionRepository

/**
 * Selector de un alimento del catálogo. Devuelve `alimentoId` y `alimentoNombre`
 * vía startActivityForResult. Permite filtrar por nombre o por categoría.
 */
class SelectorAlimentos : AppCompatActivity() {

    private val todos = mutableListOf<Alimento>()
    private val filtrados = mutableListOf<Alimento>()
    private lateinit var adapter: CatalogoAlimentosAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selector_alimentos)

        val repo = NutricionRepository(DatabaseHelper(this))

        val etBuscar = findViewById<EditText>(R.id.etBuscarAlimento)
        val rv = findViewById<RecyclerView>(R.id.rvCatalogoAlimentos)
        val btnCancelar = findViewById<Button>(R.id.btnCancelarSelectorAlim)

        todos.addAll(repo.listarAlimentos())
        filtrados.addAll(todos)

        adapter = CatalogoAlimentosAdapter(filtrados) { a ->
            val resultado = Intent().apply {
                putExtra("alimentoId", a.id)
                putExtra("alimentoNombre", a.nombre)
            }
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
                            it.categoria.lowercase().contains(q)
                    })
                }
                adapter.notifyDataSetChanged()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnCancelar.setOnClickListener { finish() }
    }

    private class CatalogoAlimentosAdapter(
        private val lista: List<Alimento>,
        private val onClick: (Alimento) -> Unit
    ) : RecyclerView.Adapter<CatalogoAlimentosAdapter.VH>() {

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvNombre: TextView = v.findViewById(R.id.tvNombreCatAlim)
            val tvCategoria: TextView = v.findViewById(R.id.tvCategoriaCatAlim)
            val tvMacros: TextView = v.findViewById(R.id.tvMacrosCatAlim)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_catalogo_alimento, parent, false)
            return VH(v)
        }

        override fun getItemCount(): Int = lista.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val a = lista[position]
            holder.tvNombre.text = a.nombre
            holder.tvCategoria.text = a.categoria
            holder.tvMacros.text = "${"%.0f".format(a.caloriasPor100g)} kcal/100g · " +
                "P ${"%.1f".format(a.proteinasPor100g)} · " +
                "C ${"%.1f".format(a.carbsPor100g)} · " +
                "G ${"%.1f".format(a.grasasPor100g)}"
            holder.itemView.setOnClickListener { onClick(a) }
        }
    }
}
