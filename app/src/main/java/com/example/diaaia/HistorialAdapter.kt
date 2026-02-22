package com.example.diaaia

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.diaaia.model.DatabaseHelper

class HistorialAdapter(
    private var lista: MutableList<RegistroItem>,
    private val dbHelper: DatabaseHelper
) : RecyclerView.Adapter<HistorialAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombreItem)
        val tvDetalle: TextView = view.findViewById(R.id.tvDetalleItem)
        val btnBorrar: ImageButton = view.findViewById(R.id.btnBorrarItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_historial, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]
        holder.tvNombre.text = item.titulo
        holder.tvDetalle.text = "${item.subtitulo} - ${item.fecha}"

        holder.btnBorrar.setOnClickListener {
            val currentPos = holder.adapterPosition
            val itemABorrar = lista[currentPos]

            // DETECCIÓN AUTOMÁTICA DE TABLA POR EMOJI
            val exito = if (itemABorrar.titulo.contains("🍎")) {
                dbHelper.borrarAlimento(itemABorrar.id) > 0
            } else {
                dbHelper.borrarEjercicio(itemABorrar.id) > 0
            }

            if (exito) {
                lista.removeAt(currentPos)
                notifyItemRemoved(currentPos)
                notifyItemRangeChanged(currentPos, lista.size)
                Toast.makeText(holder.itemView.context, "Eliminado correctamente", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(holder.itemView.context, "Error al borrar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemCount() = lista.size
}

data class RegistroItem(val id: Int, val titulo: String, val subtitulo: String, val fecha: String)